import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const API = import.meta.env.VITE_API_URL || 'http://localhost:8080/api/v1';
const savedUser = () => { try { return JSON.parse(localStorage.getItem('kinetix-session')); } catch { return null; } };

async function api(path, { token, ...options } = {}) {
  const response = await fetch(`${API}${path}`, {
    ...options,
    headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers },
  });
  if (response.status === 204) return null;
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.mensagem || body?.message || `Erro ${response.status}`);
  return body;
}
const dateOnly = (date) => new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const prettyDate = (value) => new Date(value).toLocaleString('pt-BR', { dateStyle: 'medium', timeStyle: 'short' });

function App() {
  const [session, setSession] = useState(savedUser);
  const [specialties, setSpecialties] = useState([]);
  const [city, setCity] = useState('');
  const [bairro, setBairro] = useState('');
  const [specialtyId, setSpecialtyId] = useState('');
  const [professionals, setProfessionals] = useState([]);
  const [selected, setSelected] = useState(null);
  const [serviceId, setServiceId] = useState('');
  const [day, setDay] = useState(dateOnly(new Date()));
  const [slots, setSlots] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [authMode, setAuthMode] = useState('login');
  const [authRole, setAuthRole] = useState('PACIENTE');
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => { api('/especialidades').then(setSpecialties).catch((e) => setError(e.message)); search(); }, []);
  useEffect(() => { if (session) refreshAppointments(); }, [session]);

  async function run(action) {
    setBusy(true); setError(''); setMessage('');
    try { await action(); } catch (e) { setError(e.message); } finally { setBusy(false); }
  }
  async function search(event) {
    event?.preventDefault();
    await run(async () => {
      const params = new URLSearchParams();
      if (city.trim()) params.set('cidade', city.trim());
      if (bairro.trim()) params.set('bairro', bairro.trim());
      if (specialtyId) params.set('especialidadeId', specialtyId);
      setProfessionals(await api(`/profissionais?${params}`));
      setSelected(null); setSlots([]);
    });
  }
  async function searchNearby() {
    if (!navigator.geolocation) { setError('A localização não está disponível neste navegador.'); return; }
    await run(async () => {
      const position = await new Promise((resolve, reject) => navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: false, timeout: 10000 }));
      const params = new URLSearchParams({ latitude: String(position.coords.latitude), longitude: String(position.coords.longitude), raioKm: '15' });
      setProfessionals(await api(`/profissionais?${params}`)); setSelected(null); setSlots([]);
    });
  }
  async function openProfessional(id) {
    await run(async () => { setSelected(await api(`/profissionais/${id}`)); setServiceId(''); setSlots([]); });
  }
  async function findSlots(event) {
    event.preventDefault();
    await run(async () => {
      const from = new Date(`${day}T12:00:00`);
      const until = new Date(from); until.setDate(until.getDate() + 6);
      const params = new URLSearchParams({ servicoId: serviceId, de: day, ate: dateOnly(until) });
      setSlots(await api(`/profissionais/${selected.profissional.id}/horarios?${params}`));
    });
  }
  async function book(start) {
    if (!session) { setMessage('Entre ou crie sua conta de paciente para solicitar o horário.'); return; }
    if (session.perfil !== 'PACIENTE') { setError('O agendamento deve ser feito por uma conta de paciente.'); return; }
    await run(async () => {
      await api('/consultas', { method: 'POST', token: session.token, body: JSON.stringify({
        fisioterapeutaId: selected.profissional.id, servicoId: Number(serviceId), inicio: start,
      }) });
      setMessage('Solicitação enviada. O fisioterapeuta confirmará sua consulta.');
      setSlots([]); await refreshAppointments();
    });
  }
  async function refreshAppointments() {
    if (!session?.token || session.perfil === 'ADMIN') return;
    await run(async () => setAppointments(await api('/consultas/minhas', { token: session.token })));
  }
  async function authenticate(event) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const registration = authMode === 'register';
    const payload = {
      nome: form.get('nome') || undefined, email: form.get('email'), senha: form.get('senha'),
      telefone: form.get('telefone') || undefined,
      ...(authRole === 'FISIO' ? {
        crefitoNumero: form.get('crefitoNumero'), crefitoUf: form.get('crefitoUf'),
        cidade: form.get('cidadeFisio'), uf: form.get('ufFisio'), atendeOnline: form.get('atendeOnline') === 'on', especialidadesIds: [],
      } : { cidade: form.get('cidadeCadastro') || undefined }),
    };
    await run(async () => {
      const route = registration ? `/auth/${authRole === 'FISIO' ? 'fisioterapeutas' : 'pacientes'}` : '/auth/login';
      const auth = await api(route, { method: 'POST', body: JSON.stringify(payload) });
      localStorage.setItem('kinetix-session', JSON.stringify(auth)); setSession(auth); setMessage(`Bem-vindo(a), ${auth.nome}.`);
    });
  }
  async function cancelAppointment(id) {
    const motivo = window.prompt('Por que deseja cancelar?', 'Imprevisto pessoal');
    if (!motivo?.trim()) return;
    await run(async () => { await api(`/consultas/${id}/cancelar`, { method: 'PATCH', token: session.token, body: JSON.stringify({ motivo }) }); await refreshAppointments(); });
  }
  async function updateAppointment(id, action) {
    await run(async () => { await api(`/consultas/${id}/${action}`, { method: action === 'confirmar' ? 'POST' : 'PATCH', token: session.token }); await refreshAppointments(); });
  }
  function logout() { localStorage.removeItem('kinetix-session'); setSession(null); setAppointments([]); }

  return <div className="app-shell">
    <header className="topbar"><a className="brand" href="#top"><span className="brand-mark">K</span><span>Kinetix<small>cuidado em movimento</small></span></a>
      <nav><a href="#buscar">Encontrar profissional</a>{session && <a href="#agenda">Minha agenda</a>}</nav>
      {session ? <button className="account" onClick={logout}>{session.nome} · Sair</button> : <a className="button button-light" href="#conta">Entrar</a>}
    </header>
    <main id="top">
      <section className="hero"><div className="hero-copy"><span className="eyebrow">FISIOTERAPIA DO SEU JEITO</span><h1>Movimento é parte do caminho.</h1><p>Encontre um profissional, agende seu atendimento e acompanhe sua jornada de reabilitação.</p><a className="button" href="#buscar">Encontrar fisioterapeuta <span>↗</span></a></div><div className="hero-art" aria-hidden="true"><div className="art-orbit orbit-one"/><div className="art-orbit orbit-two"/><div className="art-core">K</div><span className="art-label label-top">movimento</span><span className="art-label label-bottom">bem-estar</span></div><div className="hero-footer"><span>01 — cuidado conectado</span><span>SAÚDE · MOVIMENTO · VOCÊ</span></div></section>
      <section className="section search-section" id="buscar"><div className="section-heading"><div><span className="eyebrow">COMECE POR AQUI</span><h2>Encontre quem pode ajudar.</h2></div><span className="section-index">01 / BUSCA</span></div>
        <form className="search-bar" onSubmit={search}><label>Cidade<input value={city} onChange={(e) => setCity(e.target.value)} placeholder="Ex.: Campinas" /></label><label>Bairro<input value={bairro} onChange={(e) => setBairro(e.target.value)} placeholder="Ex.: Centro" /></label><label>Especialidade<select value={specialtyId} onChange={(e) => setSpecialtyId(e.target.value)}><option value="">Todas</option>{specialties.map((s) => <option key={s.id} value={s.id}>{s.nome}</option>)}</select></label><button className="button" disabled={busy}>Buscar <span>→</span></button></form><button className="text-button location-button" type="button" onClick={searchNearby} disabled={busy}>◎  Usar minha localização e buscar até 15 km</button>
        {error && <div className="notice error">{error}</div>}{message && <div className="notice success">{message}</div>}
        <div className="results-layout"><div className="professional-grid">{professionals.map((p) => <article className="professional-card" key={p.id}><div className="card-avatar">{p.nome?.slice(0, 1)?.toUpperCase()}</div><div className="card-meta"><span>{p.especialidades?.join(' · ') || 'Fisioterapia'}</span><strong>{p.avaliacaoMedia?.toFixed(1) || '—'} <small>★</small></strong></div><h3>{p.nomeClinica || p.nome}</h3><p>{p.nomeClinica ? `${p.nome} · ` : ''}{[p.bairro, p.cidade, p.uf].filter(Boolean).join(', ')}</p><button className="text-button" onClick={() => openProfessional(p.id)}>Ver perfil <span>↗</span></button></article>)}{!professionals.length && <div className="empty-state">Nenhum profissional encontrado com esses filtros. Experimente outra cidade ou especialidade.</div>}</div>
          {selected && <aside className="booking-panel"><button className="panel-close" onClick={() => setSelected(null)} aria-label="Fechar">×</button><span className="eyebrow">PERFIL PROFISSIONAL</span><h3>{selected.profissional.nomeClinica || selected.profissional.nome}</h3><p>{selected.profissional.apresentacao || `Fisioterapeuta ${selected.profissional.nome}, atendimento em ${selected.profissional.cidade}/${selected.profissional.uf}.`}</p><div className="tag-list">{selected.profissional.especialidades?.map((s) => <span key={s}>{s}</span>)}</div><form onSubmit={findSlots} className="booking-form"><label>Serviço<select required value={serviceId} onChange={(e) => setServiceId(e.target.value)}><option value="">Escolha um serviço</option>{selected.servicos?.map((s) => <option key={s.id} value={s.id}>{s.nome} · {s.duracaoMinutos} min · R$ {Number(s.preco).toFixed(2)}</option>)}</select></label><label>A partir de<input type="date" value={day} min={dateOnly(new Date())} onChange={(e) => setDay(e.target.value)} /></label><button className="button button-full" disabled={!serviceId || busy}>Ver horários</button></form>{slots.length > 0 && <div className="slot-list"><strong>Horários disponíveis</strong>{slots.slice(0, 12).map((slot) => <button key={slot} onClick={() => book(slot)}>{prettyDate(slot)} <span>Solicitar →</span></button>)}</div>}{selected.avaliacoes?.length > 0 && <div className="reviews"><strong>Avaliações</strong>{selected.avaliacoes.slice(0, 3).map((r) => <p key={r.id}>★ {r.nota} {r.comentario}</p>)}</div>}</aside>}
        </div>
      </section>
      <section className="section account-section" id="conta"><div className="section-heading"><div><span className="eyebrow">SUA JORNADA</span><h2>{session ? 'Sua conta Kinetix.' : 'Entre para continuar.'}</h2></div><span className="section-index">02 / CONTA</span></div>
        {!session ? <div className="account-card"><div className="account-tabs"><button className={authMode === 'login' ? 'active' : ''} onClick={() => setAuthMode('login')}>Entrar</button><button className={authMode === 'register' ? 'active' : ''} onClick={() => setAuthMode('register')}>Criar conta</button></div><form className="account-form" onSubmit={authenticate}>{authMode === 'register' && <><label>Quero me cadastrar como<select value={authRole} onChange={(e) => setAuthRole(e.target.value)}><option value="PACIENTE">Paciente</option><option value="FISIO">Fisioterapeuta</option></select></label><label>Nome completo<input name="nome" required /></label><label>Telefone<input name="telefone" /></label>{authRole === 'FISIO' ? <><label>Número CREFITO<input name="crefitoNumero" required /></label><div className="form-row"><label>UF CREFITO<input name="crefitoUf" maxLength="2" required /></label><label>UF da clínica<input name="ufFisio" maxLength="2" required /></label></div><label>Cidade da clínica<input name="cidadeFisio" required /></label><label className="check-label"><input name="atendeOnline" type="checkbox" /> Atendimento online</label><p className="form-hint">O perfil fica pendente até a equipe verificar seu CREFITO.</p></> : <label>Cidade<input name="cidadeCadastro" /></label>}</>}
          <label>E-mail<input type="email" name="email" required autoComplete="email" /></label><label>Senha<input type="password" name="senha" required minLength="8" autoComplete={authMode === 'login' ? 'current-password' : 'new-password'} /></label><button className="button button-full" disabled={busy}>{authMode === 'register' ? 'Criar minha conta' : 'Entrar na Kinetix'} <span>→</span></button></form></div> : <div className="account-card signed-in"><span className="eyebrow">CONECTADO COMO {session.perfil}</span><h3>Olá, {session.nome}.</h3><p>Atualize seu perfil e acompanhe sua agenda.</p>{session.perfil === 'PACIENTE' && <PatientProfile session={session} />}{session.perfil === 'FISIO' && <PhysioProfile session={session} />}{session.perfil === 'ADMIN' && <p>Use o painel administrativo da API para verificar cadastros e moderar avaliações.</p>}<a className="text-button" href="#agenda">Ver minha agenda ↓</a></div>}
      </section>
      {session && <section className="section agenda-section" id="agenda"><div className="section-heading"><div><span className="eyebrow">ACOMPANHAMENTO</span><h2>Minha agenda.</h2></div><button className="text-button" onClick={refreshAppointments}>Atualizar ↻</button></div><div className="appointment-list">{appointments.map((a) => <article className="appointment-card" key={a.id}><div className="appointment-date"><strong>{new Date(a.inicio).toLocaleDateString('pt-BR', { day: '2-digit' })}</strong><span>{new Date(a.inicio).toLocaleDateString('pt-BR', { month: 'short' })}</span></div><div className="appointment-info"><span>{a.status}</span><h3>{a.servicoNome}</h3><p>{session.perfil === 'PACIENTE' ? a.fisioterapeutaNome : a.pacienteNome} · {prettyDate(a.inicio)}</p></div><div className="appointment-actions">{session.perfil === 'FISIO' && a.status === 'SOLICITADA' && <button onClick={() => updateAppointment(a.id, 'confirmar')}>Confirmar</button>}{session.perfil === 'FISIO' && a.status === 'CONFIRMADA' && <button onClick={() => updateAppointment(a.id, 'concluir')}>Concluir</button>}{session.perfil === 'PACIENTE' && ['SOLICITADA', 'CONFIRMADA'].includes(a.status) && <button className="quiet-button" onClick={() => cancelAppointment(a.id)}>Cancelar</button>}</div></article>)}{appointments.length === 0 && <div className="empty-state">Sua agenda aparecerá aqui após o primeiro agendamento.</div>}</div></section>}
    </main>
    <footer><a className="brand" href="#top"><span className="brand-mark">K</span><span>Kinetix<small>cuidado em movimento</small></span></a><span>Reabilitação com mais conexão.</span><span>© 2026 Kinetix</span></footer>
  </div>;
}

function PatientProfile({ session }) {
  const [profile, setProfile] = useState({});
  const [notice, setNotice] = useState('');
  useEffect(() => { api('/paciente/perfil', { token: session.token }).then(setProfile).catch((e) => setNotice(e.message)); }, [session.token]);
  function update(field, value) { setProfile((current) => ({ ...current, [field]: value })); }
  async function save(event) {
    event.preventDefault(); setNotice('');
    const fields = ['nome','telefone','documentoIdentidade','fotoUrl','dataNascimento','cidade','bairro','uf'];
    const body = Object.fromEntries(fields.map((field) => [field, profile[field] || null]));
    try { const saved = await api('/paciente/perfil', { method: 'PUT', token: session.token, body: JSON.stringify(body) }); setProfile(saved); setNotice('Perfil atualizado.'); }
    catch (e) { setNotice(e.message); }
  }
  return <form className="management-form" onSubmit={save}><span className="eyebrow">MEU PERFIL</span><label>Nome<input value={profile.nome || ''} onChange={(e) => update('nome', e.target.value)} /></label><div className="form-row"><label>Telefone<input value={profile.telefone || ''} onChange={(e) => update('telefone', e.target.value)} /></label><label>Data de nascimento<input type="date" value={profile.dataNascimento || ''} onChange={(e) => update('dataNascimento', e.target.value)} /></label></div><div className="form-row"><label>Cidade<input value={profile.cidade || ''} onChange={(e) => update('cidade', e.target.value)} /></label><label>Bairro<input value={profile.bairro || ''} onChange={(e) => update('bairro', e.target.value)} /></label></div><label>UF<input maxLength="2" value={profile.uf || ''} onChange={(e) => update('uf', e.target.value.toUpperCase())} /></label><button className="button" disabled={!profile.usuarioId}>Salvar perfil</button>{notice && <small>{notice}</small>}</form>;
}

function PhysioProfile({ session }) {
  const [profile, setProfile] = useState({});
  const [services, setServices] = useState([]);
  const [availability, setAvailability] = useState([]);
  const [plans, setPlans] = useState([]);
  const [subscriptions, setSubscriptions] = useState([]);
  const [planId, setPlanId] = useState('');
  const [notice, setNotice] = useState('');
  const [service, setService] = useState({ nome: '', descricao: '', duracaoMinutos: 50, preco: '' });
  const [schedule, setSchedule] = useState({ diaSemana: 1, horaInicio: '09:00', horaFim: '17:00', vigenciaInicio: dateOnly(new Date()), vigenciaFim: '' });
  async function load() {
    const [p, s, a, availablePlans, currentSubscriptions] = await Promise.all([
      api('/fisioterapeuta/perfil', { token: session.token }), api('/fisioterapeuta/servicos', { token: session.token }),
      api('/fisioterapeuta/disponibilidades', { token: session.token }),
      api('/planos-assinatura'), api('/fisioterapeuta/assinaturas', { token: session.token }),
    ]);
    setProfile(p); setServices(s); setAvailability(a); setPlans(availablePlans); setSubscriptions(currentSubscriptions);
  }
  useEffect(() => { load().catch((e) => setNotice(e.message)); }, [session.token]);
  function updateProfile(field, value) { setProfile((current) => ({ ...current, [field]: value })); }
  async function submit(event, path, body, success) {
    event.preventDefault(); setNotice('');
    try { await api(path, { method: 'PUT', token: session.token, body: JSON.stringify(body) }); setNotice(success); await load(); }
    catch (e) { setNotice(e.message); }
  }
  async function addService(event) {
    event.preventDefault(); setNotice('');
    try { await api('/fisioterapeuta/servicos', { method: 'POST', token: session.token, body: JSON.stringify({ ...service, duracaoMinutos: Number(service.duracaoMinutos), preco: Number(service.preco) }) }); setService({ nome: '', descricao: '', duracaoMinutos: 50, preco: '' }); setNotice('Serviço cadastrado.'); await load(); }
    catch (e) { setNotice(e.message); }
  }
  async function addAvailability(event) {
    event.preventDefault(); setNotice('');
    try { await api('/fisioterapeuta/disponibilidades', { method: 'POST', token: session.token, body: JSON.stringify({ ...schedule, diaSemana: Number(schedule.diaSemana), vigenciaFim: schedule.vigenciaFim || null }) }); setNotice('Disponibilidade publicada.'); await load(); }
    catch (e) { setNotice(e.message); }
  }
  async function removeService(id) {
    try { await api(`/fisioterapeuta/servicos/${id}`, { method: 'DELETE', token: session.token }); await load(); }
    catch (e) { setNotice(e.message); }
  }
  async function subscribe(event) {
    event.preventDefault(); setNotice('');
    try { await api(`/fisioterapeuta/assinaturas?planoId=${planId}`, { method: 'POST', token: session.token }); setNotice('Solicitação de assinatura enviada.'); await load(); }
    catch (e) { setNotice(e.message); }
  }
  async function saveProfile(event) {
    event.preventDefault(); setNotice('');
    const body = Object.fromEntries(['nomeClinica','apresentacao','logradouro','numero','complemento','bairro','cidade','uf','cep','atendeOnline','fotoUrl'].map((field) => [field, profile[field] ?? null]));
    body.latitude = profile.latitude ? Number(profile.latitude) : null; body.longitude = profile.longitude ? Number(profile.longitude) : null;
    try { await api('/fisioterapeuta/perfil', { method: 'PUT', token: session.token, body: JSON.stringify(body) }); setNotice('Perfil profissional atualizado.'); await load(); }
    catch (e) { setNotice(e.message); }
  }
  return <div className="management-stack"><form className="management-form" onSubmit={saveProfile}><span className="eyebrow">PERFIL DA CLÍNICA</span><label>Nome da clínica<input value={profile.nomeClinica || ''} onChange={(e) => updateProfile('nomeClinica', e.target.value)} /></label><label>Apresentação<textarea value={profile.apresentacao || ''} onChange={(e) => updateProfile('apresentacao', e.target.value)} rows="3" /></label><div className="form-row"><label>Cidade<input value={profile.cidade || ''} onChange={(e) => updateProfile('cidade', e.target.value)} /></label><label>UF<input maxLength="2" value={profile.uf || ''} onChange={(e) => updateProfile('uf', e.target.value.toUpperCase())} /></label></div><div className="form-row"><label>Bairro<input value={profile.bairro || ''} onChange={(e) => updateProfile('bairro', e.target.value)} /></label><label>Logradouro<input value={profile.logradouro || ''} onChange={(e) => updateProfile('logradouro', e.target.value)} /></label></div><div className="form-row"><label>Latitude<input type="number" step="0.000001" value={profile.latitude || ''} onChange={(e) => updateProfile('latitude', e.target.value)} /></label><label>Longitude<input type="number" step="0.000001" value={profile.longitude || ''} onChange={(e) => updateProfile('longitude', e.target.value)} /></label></div><label className="check-label"><input type="checkbox" checked={Boolean(profile.atendeOnline)} onChange={(e) => updateProfile('atendeOnline', e.target.checked)} /> Atendimento online</label><button className="button" disabled={!profile.id}>Salvar perfil</button></form>
    <form className="management-form" onSubmit={addService}><span className="eyebrow">SERVIÇOS E VALORES</span><label>Nome do serviço<input required value={service.nome} onChange={(e) => setService({ ...service, nome: e.target.value })} /></label><div className="form-row"><label>Duração (min)<input type="number" min="10" max="480" required value={service.duracaoMinutos} onChange={(e) => setService({ ...service, duracaoMinutos: e.target.value })} /></label><label>Preço (R$)<input type="number" min="0" step="0.01" required value={service.preco} onChange={(e) => setService({ ...service, preco: e.target.value })} /></label></div><label>Descrição<input value={service.descricao} onChange={(e) => setService({ ...service, descricao: e.target.value })} /></label><button className="button">Adicionar serviço</button>{services.map((s) => <div className="management-row" key={s.id}><span>{s.nome} · R$ {Number(s.preco).toFixed(2)}</span><button type="button" onClick={() => removeService(s.id)}>Desativar</button></div>)}</form>
    <form className="management-form" onSubmit={addAvailability}><span className="eyebrow">AGENDA SEMANAL</span><div className="form-row"><label>Dia (1 seg. — 7 dom.)<input type="number" min="1" max="7" value={schedule.diaSemana} onChange={(e) => setSchedule({ ...schedule, diaSemana: e.target.value })} /></label><label>Início da vigência<input type="date" required value={schedule.vigenciaInicio} onChange={(e) => setSchedule({ ...schedule, vigenciaInicio: e.target.value })} /></label></div><div className="form-row"><label>Das<input type="time" required value={schedule.horaInicio} onChange={(e) => setSchedule({ ...schedule, horaInicio: e.target.value })} /></label><label>Às<input type="time" required value={schedule.horaFim} onChange={(e) => setSchedule({ ...schedule, horaFim: e.target.value })} /></label></div><button className="button">Publicar horários</button>{availability.map((a) => <small key={a.id}>{['','Segunda','Terça','Quarta','Quinta','Sexta','Sábado','Domingo'][a.diaSemana]} · {a.horaInicio}–{a.horaFim}</small>)}</form>
    <form className="management-form" onSubmit={subscribe}><span className="eyebrow">PLANOS KINETIX</span><label>Plano<select required value={planId} onChange={(e) => setPlanId(e.target.value)}><option value="">Selecione um plano</option>{plans.map((p) => <option key={p.id} value={p.id}>{p.nome} · R$ {Number(p.precoMensal).toFixed(2)}/mês</option>)}</select></label><button className="button" disabled={!planId}>Solicitar assinatura</button>{subscriptions.map((s) => <small key={s.id}>{s.planoNome} · {s.status}</small>)}</form>{notice && <div className="notice success">{notice}</div>}</div>;
}

createRoot(document.getElementById('root')).render(<App />);
