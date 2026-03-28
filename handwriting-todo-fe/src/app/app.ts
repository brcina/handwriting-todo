import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import {ApiTesterComponent} from './api-tester/api-tester';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ApiTesterComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly title = signal('handwriting-todo-fe');
}
