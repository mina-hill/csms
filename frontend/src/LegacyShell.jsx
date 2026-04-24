import { useEffect, useMemo } from 'react'

import legacyBody from './legacy/legacy-body.html?raw'
import legacyScript from './legacy/legacy-script.js?raw'
import './legacy/legacy.css'

function ensureLegacyScriptLoaded() {
  if (window.__FLOCKCONTROL_LEGACY_LOADED__) return
  window.__FLOCKCONTROL_LEGACY_LOADED__ = true

  const scriptEl = document.createElement('script')
  scriptEl.type = 'text/javascript'
  scriptEl.text = legacyScript
  document.body.appendChild(scriptEl)
}

export default function LegacyShell() {
  const bodyHtml = useMemo(() => legacyBody, [])

  useEffect(() => {
    ensureLegacyScriptLoaded()
//     import('html2pdf.js')
//       .then((mod) => {
    import("html2pdf.js/dist/html2pdf.min.js").then((mod) => {
        window.__html2pdf = mod.default
      })
      .catch(() => {
        window.__html2pdf = null
      })
  }, [])

  return <div dangerouslySetInnerHTML={{ __html: bodyHtml }} />
}

