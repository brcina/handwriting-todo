import {Component, isDevMode, signal} from '@angular/core';
import {HttpClient} from '@angular/common/http';
import {FormsModule} from '@angular/forms';

interface Endpoint {
  label: string;
  method: 'GET' | 'POST';
  url: string;
  stream: boolean;
  fileRequired: boolean;
}

@Component({
  selector: 'app-api-tester',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './api-tester.html'
})
export class ApiTesterComponent {
  isDev = isDevMode();
  response = signal('');
  streaming = signal(false);
  elapsedMs = signal<number | null>(null);
  selectedFile: File | null = null;
  message = 'Tell me a joke';
  private abortController: AbortController | null = null;
  private timerStart = 0;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  endpoints: Endpoint[] = [
    { label: 'Ask',                    method: 'GET',  url: '/api/ai/ask',                    stream: false, fileRequired: false },
    { label: 'Ask Stream',             method: 'GET',  url: '/api/ai/askStream',              stream: true,  fileRequired: false },
    { label: 'Ask About Picture',      method: 'POST', url: '/api/ai/askAboutPicture',        stream: false, fileRequired: true  },
    { label: 'Ask About Picture Stream', method: 'POST', url: '/api/ai/askAboutPictureStream', stream: true,  fileRequired: true  },
  ];
  selected = this.endpoints[0];

  constructor(private http: HttpClient) {}

  private startTimer() {
    this.timerStart = performance.now();
    this.elapsedMs.set(0);
    this.timerInterval = setInterval(() => {
      this.elapsedMs.set(Math.round(performance.now() - this.timerStart));
    }, 100);
  }

  private stopTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    this.elapsedMs.set(Math.round(performance.now() - this.timerStart));
  }

  onFileChange(event: Event) {
    const input = event.target as HTMLInputElement;
    this.selectedFile = input.files?.[0] ?? null;
  }

  send() {
    this.response.set('');
    this.stopStream();
    this.startTimer();

    if (this.selected.stream) {
      this.startStream();
    } else if (this.selected.method === 'POST') {
      this.sendPost();
    } else {
      this.sendGet();
    }
  }

  private sendGet() {
    this.http
      .get<{answer: string}>(this.selected.url, { params: { message: this.message } })
      .subscribe({
        next: r => { this.response.set(r.answer); this.stopTimer(); },
        error: e => { this.response.set(`Error: ${e.message}`); this.stopTimer(); }
      });
  }

  private sendPost() {
    if (!this.selectedFile) {
      this.response.set('Error: Bitte eine Datei auswählen.');
      this.stopTimer();
      return;
    }
    const form = new FormData();
    form.append('message', this.message);
    form.append('file', this.selectedFile);
    this.http
      .post<{answer: string}>(this.selected.url, form)
      .subscribe({
        next: r => { this.response.set(r.answer); this.stopTimer(); },
        error: e => { this.response.set(`Error: ${e.message}`); this.stopTimer(); }
      });
  }

  private startStream() {
    this.streaming.set(true);
    this.abortController = new AbortController();
    const signal = this.abortController.signal;

    let fetchPromise: Promise<Response>;

    if (this.selected.fileRequired) {
      if (!this.selectedFile) {
        this.response.set('Error: Bitte eine Datei auswählen.');
        this.streaming.set(false);
        this.stopTimer();
        return;
      }
      const form = new FormData();
      form.append('message', this.message);
      form.append('file', this.selectedFile);
      fetchPromise = fetch(this.selected.url, { method: 'POST', body: form, signal });
    } else {
      const url = `${this.selected.url}?message=${encodeURIComponent(this.message)}`;
      fetchPromise = fetch(url, { signal });
    }

    fetchPromise
      .then(async res => {
        if (!res.ok) {
          const err = await res.json().catch(() => ({ error: res.statusText }));
          this.response.set(`Error ${res.status}: ${err.error ?? res.statusText}`);
          this.streaming.set(false);
          this.stopTimer();
          return;
        }
        const reader = res.body!.getReader();
        const decoder = new TextDecoder();
        let buffer = '';
        const read = () => reader.read().then(({ done, value }) => {
          if (done) { this.streaming.set(false); this.stopTimer(); return; }
          buffer += decoder.decode(value, { stream: true });
          const lines = buffer.split('\n');
          buffer = lines.pop() ?? '';
          for (const line of lines) {
            if (!line.trim()) continue;
            try {
              const chunk = JSON.parse(line);
              this.response.update(r => r + chunk.answer);
            } catch {
              this.response.update(r => r + line);
            }
          }
          read();
        });
        read();
      })
      .catch(e => {
        if (e.name === 'AbortError') return;
        this.response.set(`Error: ${e.message}`);
        this.streaming.set(false);
        this.stopTimer();
      });
  }

  stopStream() {
    this.abortController?.abort();
    this.abortController = null;
    this.streaming.set(false);
    this.stopTimer();
  }
}
