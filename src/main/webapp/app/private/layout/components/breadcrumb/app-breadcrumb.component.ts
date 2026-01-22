import {Component} from '@angular/core';
import {ActivatedRouteSnapshot, NavigationEnd, Router} from '@angular/router';
import {BehaviorSubject} from 'rxjs';
import {filter} from 'rxjs/operators';
import { LayoutService } from '../../../../shared/service/theme/app-layout.service';

interface Breadcrumb {
    label: string;
    url?: string;
}

@Component({
    selector: 'sm-breadcrumb',
    templateUrl: './app-breadcrumb.component.html'
})
export class AppBreadcrumbComponent {
    private readonly _breadcrumbs$ = new BehaviorSubject<Breadcrumb[]>([]);
    readonly breadcrumbs$ = this._breadcrumbs$.asObservable();

    constructor(
      private router: Router,
      public layoutService: LayoutService
    ) {
        this.router.events.pipe(filter((event) => event instanceof NavigationEnd)).subscribe(_ => {
            const root = this.router.routerState.snapshot.root;
            const breadcrumbs: Breadcrumb[] = [];
            this.addBreadcrumb(root, [], breadcrumbs);

            this._breadcrumbs$.next(breadcrumbs);
        });
    }

    private addBreadcrumb(route: ActivatedRouteSnapshot, parentUrl: string[], breadcrumbs: Breadcrumb[]) {
        const routeUrl = parentUrl.concat(route.url.map(url => url.path));
        const breadcrumb = route.data['breadcrumb'];
        const parentBreadcrumb = route.parent && route.parent.data ? route.parent.data['breadcrumb'] : null;

        if (breadcrumb && breadcrumb !== parentBreadcrumb) {
            breadcrumbs.push({
                label: route.data['breadcrumb'],
                url: '/' + routeUrl.join('/')
            });
        }

        if (route.firstChild) {
            this.addBreadcrumb(route.firstChild, routeUrl, breadcrumbs);
        }
    }

}
