import type { Attachment, Page, PageSummary, SavePageRequest, User } from './types'

export class ApiError extends Error {
  status: number
  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export function errMsg(e: unknown, fallback = 'Something went wrong'): string {
  if (e instanceof ApiError) return e.message
  if (e instanceof Error && e.message) return e.message
  return fallback
}

export function errStatus(e: unknown): number | undefined {
  return e instanceof ApiError ? e.status : undefined
}

function readCookie(name: string): string | undefined {
  const match = document.cookie.match(new RegExp('(?:^|; )' + name + '=([^;]*)'))
  return match ? decodeURIComponent(match[1]) : undefined
}

type Method = 'GET' | 'POST' | 'PUT' | 'DELETE'

async function request<T>(method: Method, path: string, body?: unknown): Promise<T> {
  const headers: Record<string, string> = {
    // Signals Spring Security to answer with 401 instead of a 302 login redirect.
    'X-Requested-With': 'XMLHttpRequest',
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }
  if (method !== 'GET') {
    const csrf = readCookie('XSRF-TOKEN')
    if (csrf) headers['X-XSRF-TOKEN'] = csrf
  }

  const res = await fetch(`/api${path}`, {
    method,
    headers,
    credentials: 'include',
    body: body !== undefined ? JSON.stringify(body) : undefined,
  })

  if (res.status === 204) return undefined as T
  if (!res.ok) {
    let message = `Request failed (${res.status})`
    try {
      const data = await res.json()
      if (data && typeof data.error === 'string') message = data.error
    } catch {
      // non-JSON error body — keep the default message
    }
    throw new ApiError(res.status, message)
  }
  return (await res.json()) as T
}

export const api = {
  me: () => request<User>('GET', '/me'),

  listPages: (tag?: string) =>
    request<PageSummary[]>('GET', tag ? `/pages?tag=${encodeURIComponent(tag)}` : '/pages'),
  listTags: () => request<string[]>('GET', '/tags'),
  getPage: (slug: string) => request<Page>('GET', `/pages/${encodeURIComponent(slug)}`),
  createPage: (req: SavePageRequest) => request<Page>('POST', '/pages', req),
  updatePage: (slug: string, req: SavePageRequest) =>
    request<Page>('PUT', `/pages/${encodeURIComponent(slug)}`, req),
  deletePage: (slug: string) => request<void>('DELETE', `/pages/${encodeURIComponent(slug)}`),

  listAttachments: (slug: string) =>
    request<Attachment[]>('GET', `/pages/${encodeURIComponent(slug)}/attachments`),
  uploadAttachment: async (slug: string, file: File): Promise<Attachment> => {
    const formData = new FormData()
    formData.append('file', file)
    const csrf = readCookie('XSRF-TOKEN')
    const headers: Record<string, string> = {
      'X-Requested-With': 'XMLHttpRequest',
    }
    if (csrf) headers['X-XSRF-TOKEN'] = csrf
    const res = await fetch(`/api/pages/${encodeURIComponent(slug)}/attachments`, {
      method: 'POST',
      headers,
      credentials: 'include',
      body: formData,
    })
    if (!res.ok) {
      let message = `Upload failed (${res.status})`
      try {
        const data = await res.json()
        if (data && typeof data.error === 'string') message = data.error
      } catch { /* keep default */ }
      throw new ApiError(res.status, message)
    }
    return (await res.json()) as Attachment
  },
  attachmentUrl: (slug: string, attachmentId: string) =>
    `/api/pages/${encodeURIComponent(slug)}/attachments/${encodeURIComponent(attachmentId)}/data`,

  /** Fetches binary attachment data and returns a blob: URL suitable for <img src>. */
  fetchAttachmentBlobUrl: async (slug: string, attachmentId: string): Promise<string> => {
    const res = await fetch(
      `/api/pages/${encodeURIComponent(slug)}/attachments/${encodeURIComponent(attachmentId)}/data`,
      { credentials: 'include' },
    )
    if (!res.ok) {
      let message = `Failed to load attachment (${res.status})`
      try {
        const data = await res.json()
        if (data && typeof data.error === 'string') message = data.error
      } catch { /* keep default */ }
      throw new ApiError(res.status, message)
    }
    const blob = await res.blob()
    return URL.createObjectURL(blob)
  },
  deleteAttachment: (slug: string, attachmentId: string) =>
    request<void>('DELETE', `/pages/${encodeURIComponent(slug)}/attachments/${encodeURIComponent(attachmentId)}`),

  adminListUsers: () => request<User[]>('GET', '/admin/users'),
  adminApprove: (id: string) => request<User>('POST', `/admin/users/${id}/approve`),
  adminRevoke: (id: string) => request<User>('POST', `/admin/users/${id}/revoke`),
}

/** Full-page redirect into the Keycloak OIDC login flow. */
export function login(): void {
  window.location.href = '/api/oauth2/authorization/keycloak'
}

/** Ends the session server-side, then returns to the app. */
export async function logout(): Promise<void> {
  await request<void>('POST', '/logout')
  window.location.href = '/'
}
