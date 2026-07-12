import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { nextTick } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import PageView from './PageView.vue'
import { api } from '../api'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/pages/:slug', component: PageView },
    { path: '/pages/:slug/edit', component: { template: '<div>edit</div>' } },
    { path: '/', component: { template: '<div>home</div>' } },
  ],
})

describe('PageView attachments', () => {
  beforeEach(() => {
    router.push('/pages/my-page')
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  it('renders image attachments as <img> tags with a blob URL', async () => {
    const pageData = {
      slug: 'my-page',
      title: 'Test Page',
      content: 'Hello',
      tags: [],
      attachments: [{ id: 'img-1', filename: 'photo.png', contentType: 'image/png', size: 1024 }],
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-02T00:00:00Z',
      updatedBy: 'tester',
    }

    const getPageSpy = vi.spyOn(api, 'getPage').mockResolvedValue(pageData)
    const fetchBlobSpy = vi.spyOn(api, 'fetchAttachmentBlobUrl').mockResolvedValue('blob:mock-img-1')

    const wrapper = mount(PageView, {
      props: { slug: 'my-page' },
      global: { plugins: [router] },
    })

    // Wait for useAsyncData to load and Vue to re-render
    await flushPromises()
    await nextTick()
    // Wait for the blob fetch to complete and Vue to re-render
    await flushPromises()
    await nextTick()

    // Verify the production code path: component called api.getPage with the correct slug
    expect(getPageSpy).toHaveBeenCalledWith('my-page')

    // Verify blob URL was fetched for the image attachment
    expect(fetchBlobSpy).toHaveBeenCalledWith('my-page', 'img-1')

    const img = wrapper.find('img.attachment-image')
    expect(img.exists()).toBe(true)
    expect(img.attributes('src')).toBe('blob:mock-img-1')
    expect(img.attributes('alt')).toBe('photo.png')
  })

  it('renders non-image attachments as download links', async () => {
    const pageData = {
      slug: 'my-page',
      title: 'Test Page',
      content: 'Hello',
      tags: [],
      attachments: [{ id: 'pdf-1', filename: 'doc.pdf', contentType: 'application/pdf', size: 5120 }],
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-02T00:00:00Z',
      updatedBy: 'tester',
    }

    const getPageSpy = vi.spyOn(api, 'getPage').mockResolvedValue(pageData)
    // fetchAttachmentBlobUrl should NOT be called for non-image attachments

    const wrapper = mount(PageView, {
      props: { slug: 'my-page' },
      global: { plugins: [router] },
    })

    await flushPromises()
    await nextTick()

    // Verify the production code path: component called api.getPage with the correct slug
    expect(getPageSpy).toHaveBeenCalledWith('my-page')

    const link = wrapper.find('a.attachment-file')
    expect(link.exists()).toBe(true)
    // Use the real api.attachmentUrl to build the expected URL — this exercises
    // the production code rather than re-implementing URL construction inline.
    expect(link.attributes('href')).toBe(api.attachmentUrl('my-page', 'pdf-1'))
    expect(link.text()).toContain('doc.pdf')
    expect(link.text()).toContain('5.0 KB')
  })

  it('opens a full-size overlay when an image is clicked and closes on overlay click', async () => {
    const pageData = {
      slug: 'my-page',
      title: 'Test Page',
      content: 'Hello',
      tags: [],
      attachments: [{ id: 'img-1', filename: 'photo.png', contentType: 'image/png', size: 1024 }],
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-02T00:00:00Z',
      updatedBy: 'tester',
    }

    vi.spyOn(api, 'getPage').mockResolvedValue(pageData)
    vi.spyOn(api, 'fetchAttachmentBlobUrl').mockResolvedValue('blob:mock-img-1')

    const wrapper = mount(PageView, {
      props: { slug: 'my-page' },
      global: { plugins: [router] },
    })

    await flushPromises()
    await nextTick()
    await flushPromises()
    await nextTick()

    // Click the image to open the zoom overlay
    const img = wrapper.find('img.attachment-image')
    expect(img.exists()).toBe(true)
    await img.trigger('click')

    // The overlay should now be visible, teleported to body
    const overlay = document.querySelector('.zoom-overlay') as HTMLElement
    expect(overlay).not.toBeNull()
    const overlayImg = overlay!.querySelector('.zoom-image') as HTMLImageElement
    expect(overlayImg).not.toBeNull()
    expect(overlayImg.src).toBe('blob:mock-img-1')

    // Click the overlay to close the zoom
    overlay!.click()
    await nextTick()

    // The overlay should be gone
    expect(document.querySelector('.zoom-overlay')).toBeNull()
  })

  it('shows an error placeholder when an image fails to load', async () => {
    const pageData = {
      slug: 'my-page',
      title: 'Test Page',
      content: 'Hello',
      tags: [],
      attachments: [{ id: 'bad-img', filename: 'broken.jpg', contentType: 'image/jpeg', size: 99 }],
      createdAt: '2025-01-01T00:00:00Z',
      updatedAt: '2025-01-02T00:00:00Z',
      updatedBy: 'tester',
    }

    const getPageSpy = vi.spyOn(api, 'getPage').mockResolvedValue(pageData)
    vi.spyOn(api, 'fetchAttachmentBlobUrl').mockRejectedValue(new Error('Not found'))

    const wrapper = mount(PageView, {
      props: { slug: 'my-page' },
      global: { plugins: [router] },
    })

    await flushPromises()
    await nextTick()
    await flushPromises()
    await nextTick()

    // Verify the production code path: component called api.getPage with the correct slug
    expect(getPageSpy).toHaveBeenCalledWith('my-page')

    const errorDiv = wrapper.find('.attachment-image-error')
    expect(errorDiv.exists()).toBe(true)
    expect(errorDiv.text()).toBe('Not found')
  })
})
