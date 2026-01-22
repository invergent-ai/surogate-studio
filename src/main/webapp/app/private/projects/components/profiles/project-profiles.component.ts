import {Component, EventEmitter, Input, OnChanges, OnInit, Output, SimpleChanges} from '@angular/core';
import {NgClass, NgForOf, NgIf} from '@angular/common';
import {Profile} from "../../../../shared/model/enum/profile.model";
import {displayWarning} from '../../../../shared/util/success.util';
import {Store} from '@ngxs/store';

@Component({
  selector: 'sm-project-profiles',
  standalone: true,
  styleUrls: ['./project-profiles.component.scss'],
  templateUrl: './project-profiles.component.html',
  imports: [
    NgForOf,
    NgClass,
    NgIf
  ]
})
export class ProjectProfilesComponent implements OnInit, OnChanges {
  @Input() disableFeature: boolean;
  @Input() set disabledProfiles(_disabledProfiles: string[]) {
    this.disableProfiles(_disabledProfiles);
  }
  @Input() profileId: string;
  @Input() readonly = false;
  @Output() valueChanged = new EventEmitter<Profile>();

  profiles = [
    {
      id: Profile.HYBRID,
      name: 'Default',
      selected: true,
      disabled: false,
      icon: 'pi-globe',
      desc1: 'A mix of Data Center and Edge, suitable for most applications',
      desc2: 'The ideal choice for balancing high performance and affordability.',
    },
    {
      id: Profile.HPC,
      name: 'Data Center',
      selected: false,
      disabled: false,
      icon: 'pi-server',
      desc1: 'High-performance bare-metal servers housed in the same datacenter, ideal for mission-critical enterprise applications.',
      desc2: 'Costs might be higher compared to other profiles.'
    },
    {
      id: Profile.GPU,
      name: 'GPU',
      selected: false,
      disabled: false,
      icon: 'pi-server',
      desc1: 'Designed for AI/ML applications, this profile is essential for training, fine-tuning, or deploying AI models.',
      desc2: 'Needs GPU-enabled nodes.',
    },
    {
      id: Profile.EDGE,
      name: 'Edge',
      selected: false,
      disabled: false,
      icon: 'pi-desktop',
      desc1: 'Lower-cost, less dependable compute devices, best suited for non-critical applications.',
      desc2: 'Ideal for workloads that can tolerate some downtime or latency.',
    },
    {
      id: Profile.MYNODE,
      name: 'My Nodes',
      selected: false,
      disabled: false,
      icon: 'pi-user',
      desc1: 'Your own StateMesh registered nodes',
      desc2: 'As a StateMesh node operator, you can choose your own compute resources to enhance reliability and reduce costs.',
      desc3: 'Everything you pay for compute resources goes directly to your wallet'
    }
  ];
  Profile = Profile;

  selectedProfile: any;

  constructor(private store: Store) {}

  async ngOnInit() {
    this.profileSelected({id: this.profileId ?? Profile.HYBRID});
  }

  async ngOnChanges(changes: SimpleChanges) {
    if (changes && changes.profileId) {
      this.selectProfile({id: this.profileId ?? Profile.HYBRID});
    }
  }

  disableProfiles(_disabledProfiles: string[]): void {
    if (_disabledProfiles?.length) {
      this.profiles.forEach(profile => {
        if (_disabledProfiles.indexOf(profile.id) >= 0) {
          profile.disabled = true;
        }
      });

      if (this.profileId) {
        const selected =
          this.profiles.filter(profile => profile.id === this.profileId);
        if (selected[0].disabled) { // The already selected profile became unavailable (was disabled) => back to default
          this.profileSelected({id: Profile.HYBRID});
        }
      }
    }
  }

  profileSelected(profile: any) {
    if (this.readonly && profile?.id !== this.selectedProfile?.id) {
      displayWarning(this.store, "You can't change the profile while resources are already deployed!")
      return;
    }
    if (profile.disabled) {
      return;
    }
    this.selectProfile(profile);
    this.valueChanged.emit(this.selectedProfile?.id);
  }

  selectProfile(profile: any) {
    this.profiles.forEach(p => p.selected = profile.id === p.id);
    this.selectedProfile = this.profiles.filter(p => p.selected)[0];
  }
}
