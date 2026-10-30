const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:7001'

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

export async function apiFetch<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...init?.headers,
    },
  })

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const message = body?.messages?.join(', ') ?? `İstek başarısız oldu (${response.status})`
    throw new ApiError(message, response.status)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

async function failureOf(response: Response): Promise<ApiError> {
  const body = await response.json().catch(() => null)
  return new ApiError(
    body?.messages?.join(', ') ?? `İstek başarısız oldu (${response.status})`,
    response.status,
  )
}

/**
 * Multipart upload. Deliberately does not go through `apiFetch`: that sets a JSON
 * content type, and a multipart body needs the browser to set its own header so the
 * boundary it generated is in it.
 */
export async function apiUpload<T>(
  path: string,
  body: FormData,
  headers: Record<string, string>,
): Promise<T> {
  const response = await fetch(`${API_BASE_URL}${path}`, { method: 'POST', headers, body })
  if (!response.ok) {
    throw await failureOf(response)
  }
  return response.json() as Promise<T>
}

/**
 * Bytes rather than JSON — for images that need an Authorization header and so cannot be
 * loaded by pointing an `<img>` at the URL. The caller owns the returned object URL and
 * must revoke it, or the blob stays in memory for the life of the tab.
 */
export async function apiFetchObjectUrl(
  path: string,
  headers: Record<string, string>,
): Promise<string> {
  const response = await fetch(`${API_BASE_URL}${path}`, { headers })
  if (!response.ok) {
    throw await failureOf(response)
  }
  return URL.createObjectURL(await response.blob())
}
