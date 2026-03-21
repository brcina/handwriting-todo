import {Component, inject} from '@angular/core';
import {FormsModule} from '@angular/forms';
import {AiService} from '../../services/ai-service';

@Component({
  selector: 'app-ai-ask',
  imports: [
    FormsModule
  ],
  templateUrl: './ai-ask.html',
  styleUrl: './ai-ask.css',
})
export class AiAsk {
  question!: string;
  answer!: string;
  aiService = inject(AiService);

  onAsk() {
      this.answer = this.aiService.ask(this.question);
  }
}
