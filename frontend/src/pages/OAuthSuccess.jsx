import { useEffect } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'

function OAuthSuccess() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    const token = searchParams.get('token')

    if (token) {
      localStorage.setItem('token', token)
      navigate('/vault')
    } else {
      navigate('/login')
    }
  }, [navigate, searchParams])

  return (
    <div className="container">
      <div className="card">
        <div className="logo">🔐</div>
        <h1>Signing in...</h1>
        <p className="subtitle">
          Completing Google login
        </p>
      </div>
    </div>
  )
}

export default OAuthSuccess