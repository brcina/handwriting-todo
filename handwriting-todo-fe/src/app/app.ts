import { Component } from '@angular/core';
import { HandwritingTodoComponent } from './handwriting-todo/handwriting-todo';

@Component({
  selector: 'app-root',
  imports: [HandwritingTodoComponent],
  templateUrl: './app.html',
})
export class App {}
