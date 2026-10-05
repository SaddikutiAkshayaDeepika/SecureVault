import { Routes, Route } from 'react-router-dom'

import Login from './pages/Login'
import PasswordRecovery from './pages/PasswordRecovery'
import OAuthSuccess from './pages/OAuthSuccess'
import Signup from './pages/Signup'

import Vault from './pages/Vault'
import TeamVault from './pages/TeamVault'
import PasswordGenerator from './pages/PasswordGenerator'

import LoginHistory from './LoginHistory'
import MFA from './pages/MFA'
import SecurityAnalytics from './pages/SecurityAnalytics'
import Sessions from './pages/Sessions'
import Notifications from './pages/Notifications'
import SecureNotes from './pages/SecureNotes'

function App() {
  return (
    <Routes>

      {/* =========================
          AUTHENTICATION
      ========================= */}

      <Route
        path="/"
        element={<Login />}
      />

      <Route
        path="/login"
        element={<Login />}
      />

      <Route
        path="/signup"
        element={<Signup />}
      />

      <Route
        path="/forgot-password"
        element={<PasswordRecovery />}
      />

      <Route
        path="/reset-password"
        element={<PasswordRecovery />}
      />

      <Route
        path="/oauth-success"
        element={<OAuthSuccess />}
      />

      {/* =========================
          VAULT
      ========================= */}

      <Route
        path="/vault"
        element={<Vault />}
      />

      <Route
        path="/team-vault"
        element={<TeamVault />}
      />

      <Route
        path="/secure-notes"
        element={<SecureNotes />}
      />

      <Route
        path="/password-generator"
        element={<PasswordGenerator />}
      />

      {/* =========================
          SECURITY
      ========================= */}

      <Route
        path="/mfa"
        element={<MFA />}
      />

      <Route
        path="/login-history"
        element={<LoginHistory />}
      />

      <Route
        path="/security-analytics"
        element={<SecurityAnalytics />}
      />

      <Route
        path="/sessions"
        element={<Sessions />}
      />

      <Route
        path="/notifications"
        element={<Notifications />}
      />

    </Routes>
  )
}

export default App