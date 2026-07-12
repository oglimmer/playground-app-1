import { marked } from 'marked'
import DOMPurify from 'dompurify'

marked.setOptions({ gfm: true, breaks: false })

/**
 * Convert /PageName wiki-links to markdown links before rendering.
 *
 * Matches a slash followed by a slug-like word (alphanumeric + hyphens + underscores).
 * Skips patterns already inside URLs (://), existing markdown links ([text](/x)),
 * and code blocks or inline code (backtick-fenced).
 */
function preprocessWikiLinks(source: string): string {
  // Protect fenced code blocks and inline code spans so /PageName inside them
  // is not converted.
  const protectedBlocks: string[] = []
  const PROTECT = (match: string) => {
    protectedBlocks.push(match)
    return `WIKI${protectedBlocks.length - 1}`
  }

  let processed = source
    .replace(/```[\s\S]*?```/g, PROTECT)
    .replace(/`[^`]+`/g, PROTECT)
    // Protect existing markdown links so /PageName inside their text or
    // destination is not converted.  Covers inline links [text](url),
    // reference links [text][ref] / [text][], and image variants.
    .replace(/!?\[([^\]]*)\]\(([^)]*)\)/g, PROTECT)
    .replace(/!?\[([^\]]*)\]\[([^\]]*)\]/g, PROTECT)

  // Replace /SlugName with a markdown link.  The slash must be at the
  // start of a line or preceded by whitespace so we don't accidentally
  // match inside URLs (example.com/path), file paths (a/b), or existing
  // markdown link destinations ([text](/x)).
  processed = processed.replace(
    /(^|\s)\/([a-zA-Z0-9][a-zA-Z0-9_-]*)(?![a-zA-Z0-9_])/g,
    (_match: string, prefix: string, slug: string) =>
      `${prefix}[/${slug}](/pages/${slug})`,
  )

  // Restore protected blocks
  return processed.replace(/WIKI(\d+)/g, (_m, idx) => protectedBlocks[Number(idx)])
}

/** Render Markdown to sanitized HTML safe for v-html. */
export function renderMarkdown(source: string): string {
  const preprocessed = preprocessWikiLinks(source ?? '')
  const html = marked.parse(preprocessed, { async: false }) as string
  return DOMPurify.sanitize(html)
}
