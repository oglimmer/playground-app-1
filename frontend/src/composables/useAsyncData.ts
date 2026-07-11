import { ref, onMounted, type Ref } from 'vue'
import { errMsg } from '../api'

export interface AsyncData<T> {
  data: Ref<T>
  loading: Ref<boolean>
  error: Ref<string | null>
  reload: () => Promise<void>
}

/**
 * Loads async data with loading/error state and a reload(). Starts in a loading
 * state on mount so views never flash an empty state before the first fetch.
 */
export function useAsyncData<T>(
  loader: () => Promise<T>,
  initial: T,
  immediate = true,
): AsyncData<T> {
  const data = ref(initial) as Ref<T>
  const loading = ref(immediate)
  const error = ref<string | null>(null)

  async function reload(): Promise<void> {
    loading.value = true
    error.value = null
    try {
      data.value = await loader()
    } catch (e) {
      error.value = errMsg(e)
    } finally {
      loading.value = false
    }
  }

  if (immediate) {
    onMounted(reload)
  }

  return { data, loading, error, reload }
}
