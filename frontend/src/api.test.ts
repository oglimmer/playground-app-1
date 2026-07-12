import { afterEach, describe, expect, it, vi } from 'vitest'
import { api } from './api'

describe('api.listPages tag filtering', () => {
  afterEach(() => vi.restoreAllMocks())

  it('appends the tag query parameter', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('[]', { status: 200 }))
    await api.listPages('docs')
    expect(fetchMock).toHaveBeenCalledWith('/api/pages?tag=docs', expect.anything())
  })

  it('omits the query parameter when no tag given', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('[]', { status: 200 }))
    await api.listPages()
    expect(fetchMock).toHaveBeenCalledWith('/api/pages', expect.anything())
  })

  it('fetches the tags list endpoint', async () => {
    const fetchMock = vi
      .spyOn(globalThis, 'fetch')
      .mockResolvedValue(new Response('[]', { status: 200 }))
    await api.listTags()
    expect(fetchMock).toHaveBeenCalledWith('/api/tags', expect.anything())
  })
})
