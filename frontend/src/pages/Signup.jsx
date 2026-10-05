
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import API_URL from '../api'

function Signup() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [message, setMessage] = useState('')

  const navigate = useNavigate()

  const handleSignup = async (e) => {
    e.preventDefault()

    try {
      const response = await fetch(
        `${API_URL}/api/auth/register`,
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
        navigate('/login')
      } else {
        setMessage(result)
      }
    } catch (error) {
      setMessage('Cannot connect to backend')
    }
  }

  return (
    <div className="container">
      <div className="card">

        <div className="logo">🔐</div>

        <h1>Create Account</h1>

        <p className="subtitle">
          Create your SecureVault account
        </p>

        <form onSubmit={handleSignup}>

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

          <button className="signup" type="submit">
            Create Account
          </button>

        </form>

        {message && <p>{message}</p>}

      </div>
    </div>
  )
}

export default Signup

