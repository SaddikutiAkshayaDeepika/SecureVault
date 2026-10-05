import { useState } from 'react'

function PasswordGenerator() {
  const [password, setPassword] = useState('')
  const [length, setLength] = useState(12)

  const [uppercase, setUppercase] = useState(true)
  const [lowercase, setLowercase] = useState(true)
  const [numbers, setNumbers] = useState(true)
  const [special, setSpecial] = useState(true)

  const [strength, setStrength] = useState('')
  const [suggestion, setSuggestion] = useState('')
  const [copyMessage, setCopyMessage] = useState('')
  const [message, setMessage] = useState('')

  const checkPasswordStrength = (value) => {
    let score = 0

    if (value.length >= 12) {
      score++
    }

    if (/[A-Z]/.test(value)) {
      score++
    }

    if (/[a-z]/.test(value)) {
      score++
    }

    if (/[0-9]/.test(value)) {
      score++
    }

    if (/[^A-Za-z0-9]/.test(value)) {
      score++
    }

    if (score <= 2) {
      setStrength('Weak')

      setSuggestion(
        'Increase the password length and add uppercase letters, numbers, and special characters.'
      )
    } else if (score <= 4) {
      setStrength('Medium')

      setSuggestion(
        'Add more character variety or increase the password length to make it stronger.'
      )
    } else {
      setStrength('Strong')
      setSuggestion('This is a strong password!')
    }
  }

  const generatePassword = async () => {
    setMessage('')
    setCopyMessage('')

    if (length < 8 || length > 32) {
      setPassword('')
      setStrength('')
      setSuggestion('')
      setMessage('Password length must be between 8 and 32.')
      return
    }

    if (
      !uppercase &&
      !lowercase &&
      !numbers &&
      !special
    ) {
      setPassword('')
      setStrength('')
      setSuggestion('')
      setMessage('Select at least one character option.')
      return
    }

    try {
      const response = await fetch(
        "http://" + "localhost:8080/api/password/generate?length=" + length + "&uppercase=" + uppercase + "&lowercase=" + lowercase + "&numbers=" + numbers + "&special=" + special,

      )

      if (!response.ok) {
        const result = await response.text()

        setPassword('')
        setStrength('')
        setSuggestion('')

        setMessage(
          `Generation failed: ${response.status} ${result}`
        )

        return
      }

      const generatedPassword = await response.text()

      setPassword(generatedPassword)

      checkPasswordStrength(generatedPassword)

      setMessage('Password generated successfully')
    } catch (error) {
      console.error(error)

      setPassword('')
      setStrength('')
      setSuggestion('')
      setMessage('Cannot connect to backend')
    }
  }

  const copyPassword = async () => {
    if (!password) {
      return
    }

    try {
      await navigator.clipboard.writeText(password)
      setCopyMessage('Password copied!')
    } catch (error) {
      console.error(error)
      setCopyMessage('Unable to copy password')
    }
  }

  return (
    <div className="container">

      <div className="card">

        <div className="logo">
          🔐
        </div>

        <h1>
          Password Generator
        </h1>

        <label>
          Password Length: {length}
        </label>

        <input
          type="number"
          min="8"
          max="32"
          value={length}
          onChange={(e) =>
            setLength(Number(e.target.value))
          }
        />

        <div>
          <label>
            <input
              type="checkbox"
              checked={uppercase}
              onChange={(e) =>
                setUppercase(e.target.checked)
              }
            />
            Uppercase
          </label>
        </div>

        <div>
          <label>
            <input
              type="checkbox"
              checked={lowercase}
              onChange={(e) =>
                setLowercase(e.target.checked)
              }
            />
            Lowercase
          </label>
        </div>

        <div>
          <label>
            <input
              type="checkbox"
              checked={numbers}
              onChange={(e) =>
                setNumbers(e.target.checked)
              }
            />
            Numbers
          </label>
        </div>

        <div>
          <label>
            <input
              type="checkbox"
              checked={special}
              onChange={(e) =>
                setSpecial(e.target.checked)
              }
            />
            Special Characters
          </label>
        </div>

        <button
          className="login"
          onClick={generatePassword}
        >
          Generate Password
        </button>

        {message && (
          <p>
            {message}
          </p>
        )}

        {password && (
          <div>

            <p>
              Generated Password: {password}
            </p>

            <button
              className="login"
              onClick={copyPassword}
            >
              📋 Copy Password
            </button>

            {copyMessage && (
              <p>
                {copyMessage}
              </p>
            )}

            {strength && (
              <div>

                <p>
                  Strength:{' '}
                  <strong>
                    {strength}
                  </strong>
                </p>

                <p>
                  💡 {suggestion}
                </p>

              </div>
            )}

          </div>
        )}

      </div>

    </div>
  )
}

export default PasswordGenerator
