import {Component, OnInit} from '@angular/core';
import {InfoService} from "../../service/info.service";
import {lastValueFrom} from "rxjs";

@Component({
  standalone: true,
  selector: 'sm-website-menu',
  imports: [],
  templateUrl: './website-menu.component.html',
  styles: [`
    .site-container {
      max-width: 1190px;
      margin: 0 auto;
    }
    .navbar-container {
      align-items: center;
      height: 100%;
      padding: 1em 2em;
      display: flex;
    }
    .navbar-holder {
      width: 100%;
      height: 100%;
    }
    .brand {
      width: 150px;
    }
    .brand-image {
      width: 100%;
    }
    .nav-menu {
      justify-content: space-between;
      align-items: center;
      width: 100%;
      height: 100%;
      display: flex;
      float: right;
      position: relative;
    }
    .nav-menu-link-holder {
      justify-content: space-between;
      align-items: center;
      width: 100%;
      height: 100%;
      display: flex;
    }
    .nav-menu-link-container {
      flex: 1;
      justify-content: center;
      height: 100%;
      display: flex;
    }
    .nav-links {
      grid-column-gap: 40px;
      justify-content: flex-start;
      align-items: center;
      height: 100%;
      display: flex;
    }
    .nav-link {
      border-bottom: 3px solid #fff0;
      font-weight: 400;
      justify-content: center;
      align-items: center;
      height: 100%;
      padding: 0;
      transition: color .6s;
      display: flex;
      bottom: -1px;
      vertical-align: top;
      color: #222;
      text-align: left;
      margin-left: auto;
      margin-right: auto;
      text-decoration: none;
      position: relative;
      font-family:  Abcdiatypemono, Georgia, sans-serif;
    }

    @media screen and (max-width: 480px) {
      .no-small {
        display: none;
      }
    }
  `]
})
export class WebsiteMenuComponent implements OnInit {
  url: string;

  constructor(private infoService: InfoService) {}

  async ngOnInit() {
    const urlResponse = await lastValueFrom(this.infoService.url());
    this.url = urlResponse.body.value;
  }
}
