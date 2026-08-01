import { Component, isDevMode, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';

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
  templateUrl: './api-tester.html',
})
export class ApiTesterComponent {
  isDev = isDevMode();
  response = signal('');
  streaming = signal(false);
  elapsedMs = signal<number | null>(null);
  selectedFile: File | null = null;
  system = 'You are a standup comedian';
  message = 'Tell me a joke';
  private timerStart = 0;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  endpoints: Endpoint[] = [
    { label: 'Ask',                       method: 'GET',  url: '/api/ai/ask',                    stream: false, fileRequired: false },
    { label: 'Ask Stream',                method: 'GET',  url: '/api/ai/askStream',              stream: true,  fileRequired: false },
    { label: 'Ask About Picture',         method: 'POST', url: '/api/ai/askAboutPicture',        stream: false, fileRequired: true  },
    { label: 'Ask About Picture Stream',  method: 'POST', url: '/api/ai/askAboutPictureStream',  stream: true,  fileRequired: true  },
  ];
  selected = this.endpoints[0];

  constructor(private api: ApiService) {}

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
    this.api
      .get<{ answer: string }>(this.selected.url, { system: this.system, message: this.message })
      .then(r => { this.response.set(r.answer); this.stopTimer(); })
      .catch(e => { this.response.set(`Error: ${e.message}`); this.stopTimer(); });
  }

  private sendPost() {
    if (!this.selectedFile) {
      this.response.set('Error: Please select a file.');
      this.stopTimer();
      return;
    }
    const form = new FormData();
    form.append('system', this.system);
    form.append('message', this.message);
    form.append('file', this.selectedFile);
    this.api
      .post<{ answer: string }>(this.selected.url, form)
      .then(r => { this.response.set(r.answer); this.stopTimer(); })
      .catch(e => { this.response.set(`Error: ${e.message}`); this.stopTimer(); });
  }

  private startStream() {
    this.streaming.set(true);

    if (this.selected.fileRequired) {
      if (!this.selectedFile) {
        this.response.set('Error: Please select a file.');
        this.streaming.set(false);
        this.stopTimer();
        return;
      }
      const form = new FormData();
      form.append('system', this.system);
      form.append('message', this.message);
      form.append('file', this.selectedFile);
      this.api.startStream(
        this.selected.url,
        { body: form },
        chunk => this.response.update(r => r + chunk),
        () => { this.streaming.set(false); this.stopTimer(); },
        err => { this.response.set(`Error: ${err}`); this.streaming.set(false); this.stopTimer(); },
      );
    } else {
      this.api.startStream(
        this.selected.url,
        { params: { system: this.system, message: this.message } },
        chunk => this.response.update(r => r + chunk),
        () => { this.streaming.set(false); this.stopTimer(); },
        err => { this.response.set(`Error: ${err}`); this.streaming.set(false); this.stopTimer(); },
      );
    }
  }

  stopStream() {
    this.api.stopStream();
    this.streaming.set(false);
    this.stopTimer();
  }
}
