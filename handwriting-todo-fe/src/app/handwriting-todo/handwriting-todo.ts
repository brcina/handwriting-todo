import { Component, computed, isDevMode, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ApiService } from '../api.service';
import { ApiTesterComponent } from '../api-tester/api-tester';

type ViewState = 'idle' | 'loading' | 'view' | 'edit';

function parseTodos(markdown: string): string[] {
  return markdown
    .split('\n')
    .filter(line => /^- \[ \] .+/.test(line.trim()))
    .map(line => line.replace(/^- \[ \] /, '').trim());
}

@Component({
  selector: 'app-handwriting-todo',
  standalone: true,
  imports: [FormsModule, ApiTesterComponent],
  templateUrl: './handwriting-todo.html',
})
export class HandwritingTodoComponent {
  isDev = isDevMode();
  state = signal<ViewState>('idle');
  rawMarkdown = signal('');
  editBuffer = '';
  error = signal('');
  checked = signal<Set<number>>(new Set());
  showDevTools = signal(false);
  todos = computed(() => parseTodos(this.rawMarkdown()));
  elapsedMs = signal<number | null>(null);
  private timerStart = 0;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  constructor(private api: ApiService) {}

  private startTimer() {
    this.timerStart = performance.now();
    this.elapsedMs.set(0);
    this.timerInterval = setInterval(
      () => this.elapsedMs.set(Math.round(performance.now() - this.timerStart)),
      100,
    );
  }

  private stopTimer() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
    this.elapsedMs.set(Math.round(performance.now() - this.timerStart));
  }

  onDragOver(event: DragEvent) {
    event.preventDefault();
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    const file = event.dataTransfer?.files[0];
    if (file) this.startConversion(file);
  }

  onFileInputChange(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (file) this.startConversion(file);
  }

  private startConversion(file: File) {
    this.rawMarkdown.set('');
    this.error.set('');
    this.checked.set(new Set());
    this.state.set('loading');
    this.startTimer();

    const form = new FormData();
    form.append('file', file);

    this.api.startStream(
      '/api/handwriting/convert',
      { body: form },
      chunk => this.rawMarkdown.update(r => r + chunk),
      () => { this.stopTimer(); this.state.set('view'); },
      err => { this.stopTimer(); this.error.set(err); this.state.set('idle'); },
    );
  }

  stopConversion() {
    this.api.stopStream();
    this.stopTimer();
    this.state.set(this.rawMarkdown() ? 'view' : 'idle');
  }

  copyMarkdown() {
    navigator.clipboard.writeText(this.rawMarkdown());
  }

  enterEdit() {
    this.editBuffer = this.rawMarkdown();
    this.state.set('edit');
  }

  save() {
    // TODO: call correction API (not yet implemented)
    this.rawMarkdown.set(this.editBuffer);
    this.state.set('view');
  }

  cancel() {
    this.state.set('view');
  }

  toggleTodo(index: number) {
    const next = new Set(this.checked());
    next.has(index) ? next.delete(index) : next.add(index);
    this.checked.set(next);
  }

  reset() {
    this.api.stopStream();
    this.rawMarkdown.set('');
    this.error.set('');
    this.checked.set(new Set());
    this.editBuffer = '';
    this.state.set('idle');
  }

  toggleDevTools() {
    this.showDevTools.update(v => !v);
  }
}
