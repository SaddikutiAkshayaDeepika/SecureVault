import Login from './pages/Login'
import Vault from './pages/Vault'

function AuthPage() {
  const token = localStorage.getItem('token')

  if (token) {
    return <Vault />
  }

  return <Login />
}

export default AuthPage