import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbar } from '@angular/material/toolbar';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { map } from 'rxjs';

interface NavLink {
  path: string;
  label: string;
  icon: string;
}

@Component({
  selector: 'app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
    MatToolbar,
    MatSidenavModule,
    MatIcon,
    MatIconButton,
  ],
  templateUrl: './shell.html',
  styleUrl: './shell.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class Shell {
  protected readonly isHandset = toSignal(
    inject(BreakpointObserver)
      .observe('(max-width: 959.98px)')
      .pipe(map((state) => state.matches)),
    { initialValue: false },
  );

  protected readonly links: readonly NavLink[] = [
    { path: '/factures', label: 'Invoices', icon: 'receipt_long' },
    { path: '/reglements', label: 'Payments', icon: 'payments' },
    { path: '/produits', label: 'Products', icon: 'inventory_2' },
    { path: '/categories-produits', label: 'Product categories', icon: 'category' },
    { path: '/stocks', label: 'Stocks', icon: 'warehouse' },
    { path: '/fournisseurs', label: 'Suppliers', icon: 'local_shipping' },
    { path: '/secteurs-activite', label: 'Business sectors', icon: 'domain' },
    { path: '/operateurs', label: 'Operators', icon: 'badge' },
  ];
}
