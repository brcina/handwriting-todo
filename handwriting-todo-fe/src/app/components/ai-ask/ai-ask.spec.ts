import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AiAsk } from './ai-ask';

describe('AiAsk', () => {
  let component: AiAsk;
  let fixture: ComponentFixture<AiAsk>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AiAsk]
    })
    .compileComponents();

    fixture = TestBed.createComponent(AiAsk);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
