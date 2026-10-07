import React, { useEffect, useState } from 'react';
import { Alert, Platform, Pressable, SafeAreaView, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import Constants from 'expo-constants';
import * as Location from 'expo-location';
import * as Notifications from 'expo-notifications';

const API = process.env.EXPO_PUBLIC_API_URL || (Platform.OS === 'android' ? 'http://10.0.2.2:8080/api/v1' : 'http://localhost:8080/api/v1');
Notifications.setNotificationHandler({ handleNotification: async () => ({ shouldPlaySound: true, shouldSetBadge: false, shouldShowBanner: true, shouldShowList: true }) });

async function api(path, { token, ...options } = {}) {
  const response = await fetch(`${API}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } });
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) throw new Error(data?.mensagem || data?.message || `Erro ${response.status}`);
  return data;
}
const dateOnly = (d) => new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
const dateLabel = (v) => new Date(v).toLocaleString('pt-BR', { dateStyle: 'medium', timeStyle: 'short' });

export default function App() {
  const [session, setSession] = useState(null);
  const [guestMode, setGuestMode] = useState(false);
  const [mode, setMode] = useState('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [city, setCity] = useState('');
  const [bairro, setBairro] = useState('');
  const [specialties, setSpecialties] = useState([]);
  const [specialtyId, setSpecialtyId] = useState('');
  const [professionals, setProfessionals] = useState([]);
  const [selected, setSelected] = useState(null);
  const [serviceId, setServiceId] = useState(null);
  const [slots, setSlots] = useState([]);
  const [appointments, setAppointments] = useState([]);
  const [prescriptions, setPrescriptions] = useState([]);
  const [painEntries, setPainEntries] = useState([]);
  const [painScore, setPainScore] = useState(0);
  const [painNote, setPainNote] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => { api('/especialidades').then(setSpecialties).catch(() => {}); AsyncStorage.getItem('kinetix-session').then((value) => value && setSession(JSON.parse(value))); }, []);
  useEffect(() => { if (session?.perfil === 'PACIENTE') { loadAppointments(); loadWellness(); registerPush(); } }, [session]);
  async function action(fn) { setBusy(true); setError(''); try { await fn(); } catch (e) { setError(e.message); } finally { setBusy(false); } }
  async function authenticate() {
    await action(async () => {
      const route = mode === 'register' ? '/auth/pacientes' : '/auth/login';
      const body = mode === 'register' ? { nome: name, email, senha: password, cidade: city || undefined } : { email, senha: password };
      const data = await api(route, { method: 'POST', body: JSON.stringify(body) });
      await AsyncStorage.setItem('kinetix-session', JSON.stringify(data)); setSession(data);
    });
  }
  async function search() {
    await action(async () => { const params = new URLSearchParams(); if (city.trim()) params.set('cidade', city.trim()); if (bairro.trim()) params.set('bairro', bairro.trim()); if (specialtyId) params.set('especialidadeId', specialtyId); setProfessionals(await api(`/profissionais?${params}`)); setSelected(null); });
  }
  async function searchNearby() {
    await action(async () => {
      const permission = await Location.requestForegroundPermissionsAsync();
      if (permission.status !== 'granted') throw new Error('Permissão de localização não concedida.');
      const position = await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      const params = new URLSearchParams({ latitude: String(position.coords.latitude), longitude: String(position.coords.longitude), raioKm: '15' });
      if (specialtyId) params.set('especialidadeId', specialtyId);
      setProfessionals(await api(`/profissionais?${params}`)); setSelected(null);
    });
  }
  async function open(id) { await action(async () => { setSelected(await api(`/profissionais/${id}`)); setSlots([]); setServiceId(null); }); }
  async function getSlots() {
    if (!selected || !serviceId) return;
    await action(async () => { const start = dateOnly(new Date()); const endDate = new Date(); endDate.setDate(endDate.getDate() + 6); const params = new URLSearchParams({ servicoId: serviceId, de: start, ate: dateOnly(endDate) }); setSlots(await api(`/profissionais/${selected.profissional.id}/horarios?${params}`)); });
  }
  async function book(inicio) {
    if (!session) { setError('Entre como paciente para solicitar uma consulta.'); return; }
    await action(async () => { await api('/consultas', { method: 'POST', token: session.token, body: JSON.stringify({ fisioterapeutaId: selected.profissional.id, servicoId: Number(serviceId), inicio }) }); Alert.alert('Solicitação enviada', 'O fisioterapeuta confirmará sua consulta.'); setSlots([]); await loadAppointments(); });
  }
  async function loadAppointments() { if (session?.perfil !== 'PACIENTE') return; await action(async () => setAppointments(await api('/consultas/minhas', { token: session.token }))); }
  async function loadWellness() {
    if (session?.perfil !== 'PACIENTE') return;
    await action(async () => {
      const [plans, entries] = await Promise.all([
        api('/paciente/prescricoes', { token: session.token }),
        api('/paciente/diario-dor', { token: session.token }),
      ]);
      setPrescriptions(plans); setPainEntries(entries);
    });
  }
  async function submitPain() {
    await action(async () => {
      await api('/paciente/diario-dor', { method: 'POST', token: session.token, body: JSON.stringify({ escalaEva: painScore, observacao: painNote || undefined }) });
      setPainNote(''); await loadWellness();
    });
  }
  async function remindExercise(planoId) {
    await action(async () => {
      const enviarEm = new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString();
      await api(`/paciente/prescricoes/${planoId}/lembretes`, { method: 'POST', token: session.token, body: JSON.stringify({ enviarEm }) });
      Alert.alert('Lembrete programado', 'Você receberá uma notificação amanhã.');
    });
  }
  async function cancel(id) {
    Alert.alert('Cancelar consulta?', 'O cancelamento pelo paciente exige 24 horas de antecedência.', [
      { text: 'Voltar', style: 'cancel' },
      { text: 'Cancelar consulta', style: 'destructive', onPress: () => action(async () => { await api(`/consultas/${id}/cancelar`, { method: 'PATCH', token: session.token, body: JSON.stringify({ motivo: 'Cancelamento solicitado pelo paciente no aplicativo.' }) }); await loadAppointments(); }) },
    ]);
  }
  async function registerPush() {
    try {
      if (Platform.OS === 'android') await Notifications.setNotificationChannelAsync('kinetix-reminders', { name: 'Lembretes Kinetix', importance: Notifications.AndroidImportance.DEFAULT });
      const projectId = Constants?.expoConfig?.extra?.eas?.projectId ?? Constants?.easConfig?.projectId;
      if (!projectId) return;
      const permission = await Notifications.requestPermissionsAsync();
      if (permission.status !== 'granted') return;
      const token = (await Notifications.getExpoPushTokenAsync({ projectId })).data;
      await api('/dispositivos-push', { method: 'POST', token: session.token, body: JSON.stringify({ token, plataforma: Platform.OS === 'ios' ? 'IOS' : 'ANDROID' }) });
    } catch { /* O cadastro e a navegação seguem disponíveis sem push configurado. */ }
  }
  async function logout() { await AsyncStorage.removeItem('kinetix-session'); setSession(null); setGuestMode(false); setAppointments([]); }

  if (!session && !guestMode) return <SafeAreaView style={styles.safe}><ScrollView contentContainerStyle={styles.loginPage} keyboardShouldPersistTaps="handled"><Brand /><Text style={styles.kicker}>CUIDADO EM MOVIMENTO</Text><Text style={styles.title}>Sua jornada começa com um passo.</Text><Text style={styles.description}>Encontre fisioterapeutas verificados e acompanhe seus atendimentos.</Text><View style={styles.card}><View style={styles.switchRow}><Pressable onPress={() => setMode('login')}><Text style={mode === 'login' ? styles.switchActive : styles.switch}>Entrar</Text></Pressable><Pressable onPress={() => setMode('register')}><Text style={mode === 'register' ? styles.switchActive : styles.switch}>Criar conta</Text></Pressable></View>{mode === 'register' && <Field label="Nome completo" value={name} onChangeText={setName} autoCapitalize="words" />}<Field label="E-mail" value={email} onChangeText={setEmail} keyboardType="email-address" autoCapitalize="none" /><Field label="Senha" value={password} onChangeText={setPassword} secureTextEntry />{mode === 'register' && <Field label="Cidade (opcional)" value={city} onChangeText={setCity} />}{error ? <Text style={styles.error}>{error}</Text> : null}<PrimaryButton title={busy ? 'Aguarde…' : mode === 'login' ? 'Entrar na Kinetix' : 'Criar conta de paciente'} onPress={authenticate} disabled={busy || !email || !password || (mode === 'register' && !name)} /><Text style={styles.smallNote}>O acesso a prontuários e dados de saúde é protegido por conta autenticada.</Text></View><Pressable style={styles.guestButton} onPress={() => { setGuestMode(true); search(); }}><Text style={styles.link}>Continuar sem entrar e buscar profissionais →</Text></Pressable></ScrollView></SafeAreaView>;

  return <SafeAreaView style={styles.safe}><View style={styles.app}><View style={styles.header}><Brand />{session ? <Pressable onPress={logout}><Text style={styles.link}>Sair</Text></Pressable> : <Pressable onPress={() => setGuestMode(false)}><Text style={styles.link}>Entrar</Text></Pressable>}</View><ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled"><Text style={styles.kicker}>{session ? `OLÁ, ${session.nome?.toUpperCase()}` : 'BUSCA PÚBLICA'}</Text><Text style={styles.title}>Encontre seu profissional.</Text><View style={styles.searchRow}><View style={styles.searchFields}><TextInput style={styles.input} placeholder="Cidade" value={city} onChangeText={setCity} /><TextInput style={styles.input} placeholder="Bairro" value={bairro} onChangeText={setBairro} /></View><PrimaryButton title="Buscar" onPress={search} disabled={busy} /></View><ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={styles.specialtyChips}><Pressable style={[styles.specialtyChip, !specialtyId && styles.specialtyChipSelected]} onPress={() => setSpecialtyId('')}><Text style={[styles.specialtyText, !specialtyId && styles.specialtyTextSelected]}>Todas</Text></Pressable>{specialties.map((s) => <Pressable key={s.id} style={[styles.specialtyChip, String(s.id) === specialtyId && styles.specialtyChipSelected]} onPress={() => setSpecialtyId(String(s.id))}><Text style={[styles.specialtyText, String(s.id) === specialtyId && styles.specialtyTextSelected]}>{s.nome}</Text></Pressable>)}</ScrollView><Pressable style={styles.nearbyButton} onPress={searchNearby}><Text style={styles.link}>◎  Buscar perto de mim</Text></Pressable>{error ? <Text style={styles.error}>{error}</Text> : null}
    {selected ? <View style={styles.card}><Pressable onPress={() => setSelected(null)}><Text style={styles.link}>← Voltar aos resultados</Text></Pressable><Text style={styles.cardTitle}>{selected.profissional.nomeClinica || selected.profissional.nome}</Text><Text style={styles.description}>{selected.profissional.apresentacao || `${selected.profissional.cidade}/${selected.profissional.uf}`}</Text><Text style={styles.cardLabel}>ESCOLHA UM SERVIÇO</Text>{selected.servicos?.map((s) => <Pressable key={s.id} onPress={() => setServiceId(s.id)} style={[styles.service, serviceId === s.id && styles.serviceSelected]}><View><Text style={styles.serviceName}>{s.nome}</Text><Text style={styles.serviceMeta}>{s.duracaoMinutos} min · R$ {Number(s.preco).toFixed(2)}</Text></View><Text style={styles.radio}>{serviceId === s.id ? '●' : '○'}</Text></Pressable>)}<PrimaryButton title="Ver horários disponíveis" onPress={getSlots} disabled={busy || !serviceId} />{slots.map((slot) => <Pressable key={slot} style={styles.slot} onPress={() => book(slot)}><Text style={styles.slotText}>{dateLabel(slot)}</Text><Text style={styles.link}>Solicitar →</Text></Pressable>)}</View> : <>{professionals.map((p) => <View key={p.id} style={styles.professional}><View style={styles.avatar}><Text style={styles.avatarText}>{p.nome?.slice(0, 1)?.toUpperCase()}</Text></View><View style={styles.profInfo}><Text style={styles.profName}>{p.nomeClinica || p.nome}</Text><Text style={styles.profMeta}>{p.especialidades?.join(' · ') || 'Fisioterapia'}</Text><Text style={styles.profMeta}>{[p.bairro, p.cidade, p.uf].filter(Boolean).join(', ')}</Text><Pressable onPress={() => open(p.id)}><Text style={styles.link}>Ver perfil e horários →</Text></Pressable></View></View>)}{professionals.length === 0 && <Text style={styles.empty}>Pesquise por cidade para encontrar profissionais.</Text>}</>}
    {session?.perfil === 'PACIENTE' && <><View style={styles.agendaHeader}><Text style={styles.sectionTitle}>Minha agenda</Text><Pressable onPress={loadAppointments}><Text style={styles.link}>Atualizar ↻</Text></Pressable></View>{appointments.map((a) => <View key={a.id} style={styles.appointment}><Text style={styles.cardLabel}>{a.status}</Text><Text style={styles.serviceName}>{a.servicoNome}</Text><Text style={styles.profMeta}>{a.fisioterapeutaNome} · {dateLabel(a.inicio)}</Text>{['SOLICITADA', 'CONFIRMADA'].includes(a.status) && <Pressable onPress={() => cancel(a.id)}><Text style={styles.cancel}>Cancelar consulta</Text></Pressable>}</View>)}{appointments.length === 0 && <Text style={styles.empty}>As solicitações e consultas aparecerão aqui.</Text>}<View style={styles.agendaHeader}><Text style={styles.sectionTitle}>Reabilitação em casa</Text><Pressable onPress={loadWellness}><Text style={styles.link}>Atualizar ↻</Text></Pressable></View><View style={styles.card}><Text style={styles.cardLabel}>DIÁRIO DE DOR · ESCALA EVA 0–10</Text><View style={styles.painScale}>{Array.from({ length: 11 }, (_, value) => <Pressable key={value} onPress={() => setPainScore(value)} style={[styles.painChoice, painScore === value && styles.painChoiceSelected]}><Text style={[styles.painChoiceText, painScore === value && styles.painChoiceTextSelected]}>{value}</Text></Pressable>)}</View><TextInput style={[styles.input, styles.noteInput]} placeholder="Como você está se sentindo? (opcional)" value={painNote} onChangeText={setPainNote} multiline /><PrimaryButton title="Registrar como estou hoje" onPress={submitPain} disabled={busy} />{painEntries.slice(0, 3).map((entry) => <Text key={entry.id} style={styles.profMeta}>EVA {entry.escalaEva}/10 · {dateLabel(entry.registradoEm)}</Text>)}</View>{prescriptions.map((plan) => <View key={plan.id} style={styles.appointment}><Text style={styles.cardLabel}>PRESCRIÇÃO DE EXERCÍCIOS</Text><Text style={styles.serviceName}>{plan.titulo}</Text><Text style={styles.profMeta}>{plan.orientacoes}</Text>{plan.itens?.map((item) => <Text key={item.id} style={styles.exercise}>• {item.exercicioNome} — {item.series ? `${item.series} séries` : ''} {item.repeticoes ? `× ${item.repeticoes} repetições` : ''}{item.frequencia ? ` · ${item.frequencia}` : ''}</Text>)}<Pressable onPress={() => remindExercise(plan.id)}><Text style={styles.link}>Lembrar amanhã →</Text></Pressable></View>)}</>}{!session && <Text style={styles.empty}>A busca é pública. Entre para solicitar horários e acompanhar sua reabilitação.</Text>}</ScrollView><View style={styles.bottomNav}><Text style={styles.bottomActive}>⌂  Início</Text><Text style={styles.bottomItem}>◷  Agenda</Text><Text style={styles.bottomItem}>♡  Saúde</Text></View></View></SafeAreaView>;
}

function Brand() { return <View style={styles.brand}><View style={styles.brandIcon}><Text style={styles.brandK}>K</Text></View><View><Text style={styles.brandName}>Kinetix</Text><Text style={styles.brandSub}>CUIDADO EM MOVIMENTO</Text></View></View>; }
function Field(props) { return <View style={styles.field}><Text style={styles.fieldLabel}>{props.label}</Text><TextInput {...props} style={styles.input} placeholderTextColor="#97a69f" /></View>; }
function PrimaryButton({ title, onPress, disabled }) { return <Pressable onPress={onPress} disabled={disabled} style={[styles.primary, disabled && styles.disabled]}><Text style={styles.primaryText}>{title}</Text></Pressable>; }

const styles = StyleSheet.create({ safe: { flex: 1, backgroundColor: '#f4f3ed' }, loginPage: { padding: 25, paddingTop: 44, flexGrow: 1, justifyContent: 'center' }, guestButton: { alignItems: 'center', paddingVertical: 18 }, app: { flex: 1 }, header: { paddingHorizontal: 22, paddingVertical: 14, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', borderBottomWidth: 1, borderBottomColor: '#dce2d9' }, brand: { flexDirection: 'row', alignItems: 'center', gap: 10, marginBottom: 30 }, brandIcon: { width: 39, height: 39, borderRadius: 13, backgroundColor: '#c7ec73', alignItems: 'center', justifyContent: 'center' }, brandK: { color: '#123b34', fontSize: 22, fontWeight: '900' }, brandName: { color: '#172e29', fontSize: 19, fontWeight: '800', letterSpacing: -0.5 }, brandSub: { color: '#70817a', fontSize: 8, letterSpacing: 1.1, marginTop: 1 }, kicker: { color: '#56806c', fontSize: 10, fontWeight: '700', letterSpacing: 1.6, marginBottom: 11 }, title: { color: '#172e29', fontSize: 32, lineHeight: 39, fontWeight: '700', letterSpacing: -1.1, marginBottom: 10 }, description: { color: '#718078', fontSize: 13, lineHeight: 20, marginBottom: 18 }, card: { backgroundColor: '#fbfbf7', padding: 18, borderRadius: 10, borderWidth: 1, borderColor: '#dce2d9', marginVertical: 13 }, switchRow: { flexDirection: 'row', gap: 25, borderBottomWidth: 1, borderBottomColor: '#dce2d9', paddingBottom: 13, marginBottom: 14 }, switch: { color: '#81918a', fontSize: 13 }, switchActive: { color: '#123b34', fontWeight: '700', fontSize: 13 }, field: { gap: 6, marginBottom: 13 }, fieldLabel: { color: '#718078', fontSize: 11, fontWeight: '600' }, input: { borderWidth: 1, borderColor: '#dce2d9', backgroundColor: '#fff', borderRadius: 5, paddingHorizontal: 12, height: 45, color: '#172e29', fontSize: 14 }, primary: { backgroundColor: '#c7ec73', paddingHorizontal: 17, height: 46, borderRadius: 5, alignItems: 'center', justifyContent: 'center', marginTop: 5 }, primaryText: { color: '#123b34', fontWeight: '700', fontSize: 13 }, disabled: { opacity: 0.55 }, smallNote: { color: '#89968f', fontSize: 10, lineHeight: 15, marginTop: 15 }, error: { color: '#a0362b', backgroundColor: '#fde7e4', padding: 10, borderRadius: 4, fontSize: 12, marginVertical: 9 }, link: { color: '#1d6151', fontWeight: '700', fontSize: 11 }, content: { padding: 21, paddingBottom: 34 }, searchRow: { flexDirection: 'row', gap: 8, marginVertical: 16, alignItems: 'center' }, searchFields: { flex: 1, gap: 7 }, specialtyChips: { flexDirection: 'row', gap: 7, paddingVertical: 3 }, specialtyChip: { borderWidth: 1, borderColor: '#dce2d9', borderRadius: 16, paddingHorizontal: 10, paddingVertical: 7, backgroundColor: '#fbfbf7' }, specialtyChipSelected: { borderColor: '#1d6151', backgroundColor: '#e5f0dd' }, specialtyText: { color: '#64746b', fontSize: 9 }, specialtyTextSelected: { color: '#1d6151', fontWeight: '700' }, nearbyButton: { alignSelf: 'flex-start', paddingVertical: 7, paddingHorizontal: 2 }, professional: { backgroundColor: '#fbfbf7', borderRadius: 8, borderWidth: 1, borderColor: '#dce2d9', padding: 14, flexDirection: 'row', gap: 13, marginBottom: 10 }, avatar: { width: 43, height: 43, backgroundColor: '#dfebd6', borderRadius: 23, alignItems: 'center', justifyContent: 'center' }, avatarText: { color: '#1d6151', fontSize: 18, fontWeight: '700' }, profInfo: { flex: 1, gap: 4 }, profName: { color: '#172e29', fontWeight: '700', fontSize: 15 }, profMeta: { color: '#77867e', fontSize: 11, lineHeight: 16 }, empty: { color: '#7b8982', fontSize: 12, lineHeight: 18, paddingVertical: 15 }, cardTitle: { color: '#172e29', fontSize: 19, fontWeight: '700', marginTop: 14, marginBottom: 7 }, cardLabel: { color: '#718078', fontSize: 9, fontWeight: '700', letterSpacing: 1, marginTop: 13, marginBottom: 7 }, service: { borderWidth: 1, borderColor: '#dce2d9', borderRadius: 5, padding: 11, flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 7 }, serviceSelected: { borderColor: '#1d6151', backgroundColor: '#edf4e7' }, serviceName: { color: '#172e29', fontWeight: '700', fontSize: 13 }, serviceMeta: { color: '#77867e', fontSize: 10, marginTop: 4 }, radio: { color: '#1d6151', fontSize: 17 }, slot: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 11, borderBottomWidth: 1, borderBottomColor: '#e2e7e0' }, slotText: { color: '#51635a', fontSize: 11 }, agendaHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginTop: 28, marginBottom: 10 }, sectionTitle: { color: '#172e29', fontSize: 20, fontWeight: '700' }, appointment: { backgroundColor: '#fbfbf7', borderWidth: 1, borderColor: '#dce2d9', borderRadius: 7, padding: 13, marginBottom: 9 }, cancel: { color: '#9e564e', fontSize: 11, fontWeight: '700', marginTop: 10 }, noteInput: { minHeight: 70, paddingTop: 10, textAlignVertical: 'top', marginBottom: 8 }, painScale: { flexDirection: 'row', justifyContent: 'space-between', marginVertical: 10 }, painChoice: { width: 25, height: 30, borderRadius: 5, alignItems: 'center', justifyContent: 'center', backgroundColor: '#eef0e9' }, painChoiceSelected: { backgroundColor: '#1d6151' }, painChoiceText: { color: '#63726a', fontSize: 10 }, painChoiceTextSelected: { color: '#fff', fontWeight: '700' }, exercise: { color: '#52655b', fontSize: 11, marginVertical: 4 }, bottomNav: { minHeight: 55, borderTopWidth: 1, borderTopColor: '#dce2d9', backgroundColor: '#fbfbf7', flexDirection: 'row', alignItems: 'center', justifyContent: 'space-around' }, bottomActive: { color: '#1d6151', fontWeight: '700', fontSize: 11 }, bottomItem: { color: '#829088', fontSize: 11 } });

