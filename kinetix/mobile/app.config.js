export default {
  expo: {
    name: 'Kinetix',
    slug: 'kinetix-fisioterapia',
    version: '0.1.0',
    orientation: 'portrait',
    userInterfaceStyle: 'light',
    scheme: 'kinetix',
    plugins: [
      'expo-notifications',
      ['expo-location', { locationWhenInUsePermission: 'Permita que a Kinetix encontre profissionais perto de você.' }],
    ],
    ios: { supportsTablet: true, bundleIdentifier: 'br.com.kinetix.app' },
    android: { package: 'br.com.kinetix.app' },
    extra: { eas: { projectId: process.env.EXPO_PUBLIC_EAS_PROJECT_ID || undefined } },
  },
};
