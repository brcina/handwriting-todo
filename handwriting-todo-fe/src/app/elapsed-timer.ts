import { signal, Signal } from '@angular/core';

export interface ElapsedTimer {
  elapsedMs: Signal<number | null>;
  start(): void;
  stop(): void;
}

export function createElapsedTimer(): ElapsedTimer {
  const elapsedMs = signal<number | null>(null);
  let timerStart = 0;
  let timerInterval: ReturnType<typeof setInterval> | null = null;

  function start() {
    timerStart = performance.now();
    elapsedMs.set(0);
    timerInterval = setInterval(() => {
      elapsedMs.set(Math.round(performance.now() - timerStart));
    }, 100);
  }

  function stop() {
    if (timerInterval) {
      clearInterval(timerInterval);
      timerInterval = null;
    }
    elapsedMs.set(Math.round(performance.now() - timerStart));
  }

  return { elapsedMs, start, stop };
}
