const API_BASE_URL = 'http://localhost:8080/api'

const ensureOk = async (response) => {
  if (response.ok) {
    return response.json()
  }

  let message = `Request failed with status ${response.status}`
  try {
    const payload = await response.json()
    if (payload?.message) {
      message = payload.message
    }
  } catch {
    // Ignore JSON parsing failures and use the default message.
  }

  const error = new Error(message)
  error.status = response.status
  throw error
}

const buildQuery = (params = {}) => {
  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      searchParams.set(key, value)
    }
  })

  const queryString = searchParams.toString()
  return queryString ? `?${queryString}` : ''
}

export const fetchProducts = async (params = {}) => {
  const response = await fetch(`${API_BASE_URL}/products${buildQuery(params)}`)
  return ensureOk(response)
}

export const fetchProductById = async (id) => {
  const response = await fetch(`${API_BASE_URL}/products/${id}`)
  return ensureOk(response)
}

export const fetchCategories = async () => {
  const response = await fetch(`${API_BASE_URL}/categories`)
  return ensureOk(response)
}

