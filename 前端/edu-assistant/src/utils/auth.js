import { ref } from 'vue'

const TOKEN_KEY = 'token'
const USER_INFO_KEY = 'userInfo'

function getSessionStorage() {
  if (typeof window === 'undefined') return null
  return window.sessionStorage
}

function clearLegacyLocalAuth() {
  if (typeof window === 'undefined') return
  window.localStorage.removeItem(TOKEN_KEY)
  window.localStorage.removeItem(USER_INFO_KEY)
}

function readStoredToken() {
  return getSessionStorage()?.getItem(TOKEN_KEY) || ''
}

function readStoredUserInfo() {
  const raw = getSessionStorage()?.getItem(USER_INFO_KEY)
  if (!raw) return {}
  try {
    return JSON.parse(raw)
  } catch {
    return {}
  }
}

const tokenState = ref(readStoredToken())
const userInfoState = ref(readStoredUserInfo())

export function getToken() {
  return tokenState.value
}

export function getUserInfo() {
  return userInfoState.value
}

export function setAuthSession(token, user) {
  const storage = getSessionStorage()
  if (!storage) return
  if (token) {
    storage.setItem(TOKEN_KEY, token)
  } else {
    storage.removeItem(TOKEN_KEY)
  }
  if (user === undefined) {
    storage.removeItem(USER_INFO_KEY)
  } else {
    storage.setItem(USER_INFO_KEY, JSON.stringify(user || {}))
  }
  tokenState.value = token || ''
  userInfoState.value = user || {}
  clearLegacyLocalAuth()
}

export function clearAuthSession() {
  const storage = getSessionStorage()
  if (storage) {
    storage.removeItem(TOKEN_KEY)
    storage.removeItem(USER_INFO_KEY)
  }
  tokenState.value = ''
  userInfoState.value = {}
  clearLegacyLocalAuth()
}
