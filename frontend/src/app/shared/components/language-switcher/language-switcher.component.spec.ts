import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslatePipe, TranslateService, provideTranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { LanguageSwitcherComponent } from './language-switcher.component';

@Component({
  imports: [TranslatePipe, LanguageSwitcherComponent],
  template: `
    <app-language-switcher />
    <span id="label">{{ 'nav.dashboard' | translate }}</span>
  `,
})
class HostComponent {}

describe('LanguageSwitcherComponent', () => {
  let fixture: ComponentFixture<HostComponent>;
  let translate: TranslateService;

  function label(): string {
    return (fixture.nativeElement.querySelector('#label') as HTMLElement).textContent?.trim() ?? '';
  }

  function button(code: string): HTMLButtonElement {
    const buttons = fixture.nativeElement.querySelectorAll(
      'button',
    ) as NodeListOf<HTMLButtonElement>;
    return Array.from(buttons).find(
      (candidate) => candidate.textContent?.trim().toLowerCase() === code,
    ) as HTMLButtonElement;
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HostComponent],
      providers: [provideTranslateService({ lang: 'fr', fallbackLang: 'en' })],
    }).compileComponents();

    translate = TestBed.inject(TranslateService);
    translate.setTranslation('fr', { nav: { dashboard: 'Tableau de bord' } });
    translate.setTranslation('en', { nav: { dashboard: 'Dashboard' } });
    await firstValueFrom(translate.use('fr'));

    fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    await fixture.whenStable();
  });

  it('defaults to French and translates labels accordingly', () => {
    expect(label()).toBe('Tableau de bord');
    expect(button('fr').getAttribute('aria-pressed')).toBe('true');
  });

  it('switches the active language and re-renders a translated label', async () => {
    button('en').click();
    await firstValueFrom(translate.use('en'));
    fixture.detectChanges();
    await fixture.whenStable();

    expect(translate.getCurrentLang()).toBe('en');
    expect(label()).toBe('Dashboard');
    expect(button('en').getAttribute('aria-pressed')).toBe('true');
    expect(button('fr').getAttribute('aria-pressed')).toBe('false');
  });
});
