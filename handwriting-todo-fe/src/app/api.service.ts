import { Injectable } from '@angular/core';

export interface StreamOptions {
  params?: Record<string, string>;
  body?: FormData;
}

@Injectable({ providedIn: 'root' })
export class ApiService {
  private abortController: AbortController | null = null;

  async get<T>(url: string, params: Record<string, string> = {}): Promise<T> {
    const qs = new URLSearchParams(params).toString();
    const res = await fetch(qs ? `${url}?${qs}` : url);
    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: res.statusText }));
      throw new Error(err.error ?? res.statusText);
    }
    return res.json() as Promise<T>;
  }

  async post<T>(url: string, body: FormData): Promise<T> {
    const res = await fetch(url, { method: 'POST', body });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: res.statusText }));
      throw new Error(err.error ?? res.statusText);
    }
    return res.json() as Promise<T>;
  }

  startStream(
    url: string,
    options: StreamOptions,
    onChunk: (answer: string) => void,
    onDone: () => void,
    onError: (error: string) => void,
  ): void {
    this.abortController = new AbortController();
    const signal = this.abortController.signal;

    let fetchPromise: Promise<Response>;
    if (options.body) {
      fetchPromise = fetch(url, { method: 'POST', body: options.body, signal });
    } else {
      const qs = new URLSearchParams(options.params ?? {}).toString();
      fetchPromise = fetch(qs ? `${url}?${qs}` : url, { signal });
    }

    fetchPromise
      .then(async res => {
        if (!res.ok) {
          const err = await res.json().catch(() => ({ error: res.statusText }));
          onError(err.error ?? res.statusText);
          return;
        }
        const reader = res.body!.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        const read = (): void => {
          reader.read().then(({ done, value }) => {
            if (done) {
              onDone();
              return;
            }
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() ?? '';
            for (const line of lines) {
              if (!line.trim()) continue;
              try {
                const chunk = JSON.parse(line);
                onChunk(chunk.answer);
              } catch { /* skip malformed line */ }
            }
            read();
          });
        };
        read();
      })
      .catch(e => {
        if (e.name === 'AbortError') return;
        onError(e.message);
      });
  }

  stopStream(): void {
    this.abortController?.abort();
    this.abortController = null;
  }
}
