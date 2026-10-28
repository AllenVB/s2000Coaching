/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  /** IANA zone name; must match the server's APP_TIME_ZONE. Defaults to Europe/Istanbul. */
  readonly VITE_APP_TIME_ZONE?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
