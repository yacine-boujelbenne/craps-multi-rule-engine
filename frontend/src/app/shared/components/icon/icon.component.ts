import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type IconName =
  | 'grid'
  | 'folder'
  | 'database'
  | 'sliders'
  | 'search'
  | 'plus'
  | 'chevron-right'
  | 'arrow-left'
  | 'ship'
  | 'calendar'
  | 'building'
  | 'factory'
  | 'users'
  | 'bell'
  | 'truck'
  | 'anchor'
  | 'file'
  | 'check'
  | 'alert'
  | 'clock'
  | 'close'
  | 'menu'
  | 'map-pin'
  | 'trending-up'
  | 'inbox';

/**
 * Dependency-free inline SVG icon set (stroke style). Keeps the bundle small
 * and avoids loading an icon font at runtime.
 */
@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'app-icon' },
  template: `
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.75"
      stroke-linecap="round"
      stroke-linejoin="round"
      [attr.width]="size()"
      [attr.height]="size()"
      aria-hidden="true"
      focusable="false"
    >
      @switch (name()) {
        @case ('grid') {
          <rect x="3" y="3" width="7" height="7" rx="1.5" />
          <rect x="14" y="3" width="7" height="7" rx="1.5" />
          <rect x="3" y="14" width="7" height="7" rx="1.5" />
          <rect x="14" y="14" width="7" height="7" rx="1.5" />
        }
        @case ('folder') {
          <path
            d="M3 7.5A1.5 1.5 0 0 1 4.5 6h4l2 2.5h7A1.5 1.5 0 0 1 19 10v6.5a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 3 16.5z"
          />
        }
        @case ('database') {
          <ellipse cx="12" cy="5.5" rx="7" ry="2.5" />
          <path d="M5 5.5v13c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5v-13" />
          <path d="M5 12c0 1.4 3.1 2.5 7 2.5s7-1.1 7-2.5" />
        }
        @case ('sliders') {
          <path d="M4 6h10M18 6h2M4 12h4M12 12h8M4 18h12M20 18h0" />
          <circle cx="16" cy="6" r="2" />
          <circle cx="10" cy="12" r="2" />
          <circle cx="18" cy="18" r="2" />
        }
        @case ('search') {
          <circle cx="11" cy="11" r="7" />
          <path d="m20 20-3.2-3.2" />
        }
        @case ('plus') {
          <path d="M12 5v14M5 12h14" />
        }
        @case ('chevron-right') {
          <path d="m9 6 6 6-6 6" />
        }
        @case ('arrow-left') {
          <path d="M19 12H5M12 19l-7-7 7-7" />
        }
        @case ('ship') {
          <path d="M3 15l1.5 4.5h15L21 15" />
          <path d="M5 15V9l7-3 7 3v6" />
          <path d="M12 6V3" />
        }
        @case ('calendar') {
          <rect x="3.5" y="5" width="17" height="16" rx="2" />
          <path d="M3.5 10h17M8 3v4M16 3v4" />
        }
        @case ('building') {
          <rect x="4" y="3" width="16" height="18" rx="2" />
          <path d="M9 8h2M13 8h2M9 12h2M13 12h2M10 21v-4h4v4" />
        }
        @case ('factory') {
          <path d="M3 21V9l6 4V9l6 4V9l6 4v8z" />
          <path d="M3 21h18M8 21v-4h4v4" />
        }
        @case ('users') {
          <circle cx="9" cy="8" r="3.2" />
          <path d="M3.5 20a5.5 5.5 0 0 1 11 0" />
          <path d="M16 5.2a3.2 3.2 0 0 1 0 5.6M17 20a5.5 5.5 0 0 0-3-4.9" />
        }
        @case ('bell') {
          <path d="M6 9a6 6 0 0 1 12 0c0 5 2 6 2 6H4s2-1 2-6" />
          <path d="M10 20a2 2 0 0 0 4 0" />
        }
        @case ('truck') {
          <path d="M3 7h11v9H3zM14 10h4l3 3v3h-7z" />
          <circle cx="7" cy="18" r="1.8" />
          <circle cx="17" cy="18" r="1.8" />
        }
        @case ('anchor') {
          <circle cx="12" cy="5" r="2.2" />
          <path d="M12 7.2V21M5 12H3.5a8.5 8.5 0 0 0 17 0H19M8.5 10h7" />
        }
        @case ('file') {
          <path d="M6 3h7l5 5v13a1 1 0 0 1-1 1H6a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1z" />
          <path d="M13 3v5h5M9 13h6M9 17h6" />
        }
        @case ('check') {
          <path d="m5 13 4 4L19 7" />
        }
        @case ('alert') {
          <path d="M12 3.5 21 19H3z" />
          <path d="M12 10v4M12 17h.01" />
        }
        @case ('clock') {
          <circle cx="12" cy="12" r="8.5" />
          <path d="M12 7.5V12l3 2" />
        }
        @case ('close') {
          <path d="M6 6l12 12M18 6 6 18" />
        }
        @case ('menu') {
          <path d="M4 7h16M4 12h16M4 17h16" />
        }
        @case ('map-pin') {
          <path d="M12 21s7-6.2 7-11a7 7 0 1 0-14 0c0 4.8 7 11 7 11z" />
          <circle cx="12" cy="10" r="2.5" />
        }
        @case ('trending-up') {
          <path d="M3 17l6-6 4 4 8-8M15 7h6v6" />
        }
        @case ('inbox') {
          <path d="M3 12h5l1.5 2.5h5L16 12h5" />
          <path d="M5 5h14l2 7v6a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1v-6z" />
        }
      }
    </svg>
  `,
  styles: [
    `
      :host {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        line-height: 0;
      }
      svg {
        display: block;
      }
    `,
  ],
})
export class IconComponent {
  readonly name = input.required<IconName>();
  readonly size = input<number>(20);
}
