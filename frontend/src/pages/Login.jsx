
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import API_URL from '../api'

function Login() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [mfaCode, setMfaCode] = useState('')
  const [mfaRequired, setMfaRequired] = useState(false)
  const [mfaToken, setMfaToken] = useState('')
  const [message, setMessage] = useState('')

  const navigate = useNavigate()

  const handleLogin = async (e) => {
    e.preventDefault()
    setMessage('Logging in...')

    try {
      const response = await fetch(
        `${API_URL}/api/auth/login`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            email: email,
            password: password,
          }),
        }
      )

      const result = await response.text()

      if (response.ok) {
        const payload = JSON.parse(atob(result.split('.')[1]))

        if (payload.mfaVerified === false) {
          setMfaToken(result)
          setMfaRequired(true)
          setMessage('Enter your MFA code')
        } else {
          localStorage.setItem('token', result)
          setMessage('Login successful')

          setTimeout(() => {
            navigate('/vault')
          }, 500)
        }
      } else {
        setMessage(result)
      }

    } catch (error) {
      console.log(error)
      setMessage(error.message)
    }
  }

  const handleMfaLogin = async (e) => {
    e.preventDefault()
    setMessage('Verifying MFA code...')

    try {
      const response = await fetch(
        `${API_URL}/api/auth/mfa-login?code=${mfaCode}`,
        {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${mfaToken}`,
          },
        }
      )

      const result = await response.text()

      if (response.ok) {
        localStorage.setItem('token', result)
        setMessage('MFA verified. Login successful')

        setTimeout(() => {
          navigate('/vault')
        }, 500)
      } else {
        setMessage(result)
      }

    } catch (error) {
      console.log(error)
      setMessage(error.message)
    }
  }

  const handleGoogleLogin = () => {
    window.location.href =
      `${API_URL}/oauth2/authorization/google`
  }

  return (
    <div className="container">

      <div className="card">

        <div className="logo">🔐</div>

        <h1>Login</h1>

        <p className="subtitle">
          Welcome back to SecureVault
        </p>

        {!mfaRequired ? (

          <>
            <form onSubmit={handleLogin}>

              <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />

              <input
                type="password"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />

              <button
                type="submit"
                className="login"
              >
                Login
              </button>

            </form>

            <p>
              <button
                type="button"
                className="link-button"
                onClick={() => navigate('/forgot-password')}
              >
                Forgot Password?
              </button>
            </p>

            <p>
              <button
                type="button"
                className="link-button"
                onClick={() => navigate('/signup')}
              >
                Create Account
              </button>
            </p>

            <button
              type="button"
              className="login"
              onClick={handleGoogleLogin}
            >
              Continue with Google
            </button>
          </>

        ) : (

          <form onSubmit={handleMfaLogin}>

            <input
              type="text"
              placeholder="Enter 6-digit MFA code"
              value={mfaCode}
              onChange={(e) => setMfaCode(e.target.value)}
              maxLength="6"
              required
            />

            <button
              type="submit"
              className="login"
            >
              Verify MFA
            </button>

          </form>

        )}

        {message && (
          <p>{message}</p>
        )}

      </div>

    </div>
  )
}

export default Login

