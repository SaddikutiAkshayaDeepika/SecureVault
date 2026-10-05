
import { useEffect, useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import API_URL from '../api'

function PasswordRecovery() {
  const [email, setEmail] = useState('')
  const [token, setToken] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [resetToken, setResetToken] = useState('')
  const [step, setStep] = useState('request')
  const [message, setMessage] = useState('')
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    const urlToken = searchParams.get('token')

    if (urlToken) {
      setToken(urlToken)
      setStep('reset')
    }
  }, [searchParams])

  const handleRequestReset = async (e) => {
    e.preventDefault()
    setMessage('Requesting password reset...')

    try {
      const response = await fetch(
        `${API_URL}/api/auth/forgot-password`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            email: email,
          }),
        }
      )

      const result = await response.json()

      if (response.ok) {
        setMessage(result.message)

        if (result.resetToken) {
          setResetToken(result.resetToken)
          setToken(result.resetToken)
          setStep('reset')
        }
      } else {
        setMessage(result.message || 'Something went wrong')
      }
    } catch (error) {
      setMessage(error.message)
    }
  }

  const handleResetPassword = async (e) => {
    e.preventDefault()

    if (newPassword !== confirmPassword) {
      setMessage('Passwords do not match')
      return
    }

    setMessage('Resetting password...')

    try {
      const response = await fetch(
        `${API_URL}/api/auth/reset-password`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
          },
          body: JSON.stringify({
            token: token,
            newPassword: newPassword,
          }),
        }
      )

      const result = await response.json()

      if (response.ok) {
        setMessage('Password reset successfully. You can now login.')
        setStep('success')
      } else {
        setMessage(result.message || 'Password reset failed')
      }
    } catch (error) {
      setMessage(error.message)
    }
  }

  return (
    <div className="container">
      <div className="card">

        <div className="logo">🔐</div>

        {step === 'request' && (
          <>
            <h1>Forgot Password</h1>

            <p className="subtitle">
              Enter your registered email to reset your password
            </p>

            <form onSubmit={handleRequestReset}>

              <input
                type="email"
                placeholder="Email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
              />

              <button type="submit" className="login">
                Generate Reset Token
              </button>

            </form>

            {message && <p>{message}</p>}

            <button
              type="button"
              className="secondary-button"
              onClick={() => navigate('/login')}
            >
              Back to Login
            </button>
          </>
        )}

        {step === 'reset' && (
          <>
            <h1>Reset Password</h1>

            <p className="subtitle">
              Enter your new password
            </p>

            <form onSubmit={handleResetPassword}>

              <input
                type="text"
                placeholder="Reset Token"
                value={token}
                onChange={(e) => setToken(e.target.value)}
                required
              />

              <input
                type="password"
                placeholder="New Password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                required
              />

              <input
                type="password"
                placeholder="Confirm New Password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                required
              />

              <button type="submit" className="login">
                Reset Password
              </button>

            </form>

            {message && <p>{message}</p>}
          </>
        )}

        {step === 'success' && (
          <>
            <h1>Password Reset</h1>

            <p className="subtitle">
              Your password has been changed successfully.
            </p>

            <p>{message}</p>

            <button
              type="button"
              className="login"
              onClick={() => navigate('/login')}
            >
              Go to Login
            </button>
          </>
        )}

        {resetToken && step === 'reset' && (
          <p>
            Reset token generated successfully.
          </p>
        )}

      </div>
    </div>
  )
}

export default PasswordRecovery

