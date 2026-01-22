import {IProject} from "./project.model";
import {IZone} from "./zone.model";
import {ColorScheme, MenuMode} from "../service/theme/app-layout.service";
import dayjs from "dayjs/esm";

export class Account {
  constructor(
    public id: string,
    public activated: boolean,
    public authorities: string[],
    public email: string,
    public firstName: string | null,
    public langKey: string,
    public lastName: string | null,
    public login: string,
    public imageUrl: string | null,
    public defaultProject: IProject,
    public defaultZone: IZone,
    public lockedUserTime: dayjs.Dayjs | null,
    public lockedOperator: boolean,
    public hasApps: boolean,
    public cicdPipelineAutopublish: boolean,
    public theme: string,
    public colorScheme: ColorScheme,
    public scale: number,
    public menuMode: MenuMode,
    public ripple: boolean,
    public paymentMethod: 'card' | 'crypto',
    public credits: number,
    public dataCenter: boolean,
    public userType: string,
    public company: string,
    public taxCode: string,
    public address: string,
    public city: string,
    public state: string,
    public country: string,
    public zip: string,
    public referralCode: string
  ) {}
}


