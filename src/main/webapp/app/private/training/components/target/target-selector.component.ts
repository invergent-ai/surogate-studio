import { Component, computed, EventEmitter, input, OnInit, Output, Signal } from '@angular/core';
import { CardModule } from 'primeng/card';
import { NgClass, NgForOf, NgTemplateOutlet } from '@angular/common';
import { Check, LucideAngularModule, Rocket, ServerIcon } from 'lucide-angular';
import { TagModule } from 'primeng/tag';

@Component({
  standalone: true,
  selector: 'sm-target-selector',
  templateUrl: './target-selector.component.html',
  imports: [
    CardModule,
    NgTemplateOutlet,
    LucideAngularModule,
    TagModule,
    NgForOf,
    NgClass
  ]
})
export class TargetSelectorComponent implements OnInit {
  selected = input.required<string>();
  @Output() onSelect = new EventEmitter<string>();

  items: Signal<any[]> = computed(() => [
    {
      id: 'local',
      imgBg: "bg-purple-400",
      img: ServerIcon,
      title: "Local infrastructure",
      description: "Training jobs will be distributed across your multi-node, multi-GPU infrastructure using the current k8s cluster.",
      tags: [
        {severity: "success", value: "On-Prem", image: Check},
        {severity: "secondary", value: "Private", image: Check},
        {severity: "warning", value: "Powered by KubeRay", image: Rocket},
      ],
      selected: this.selected() === 'local'
    },
    {
      id: 'cloud',
      imgBg: "bg-orange-500",
      img: Rocket,
      title: "In the cloud",
      description: "Training jobs will be launched on configured public cloud compute resources.",
      tags: [
        {severity: "success", value: "Fast", image: Check},
        {severity: "secondary", value: "Unlimited resources", image: Check},
        {severity: "warning", value: "Powered by SkyPilot", image: Rocket},
      ],
      selected: this.selected() === 'cloud'
    },
  ]);

  ngOnInit() {}

  cardSelected(id: string) {
    this.items().forEach(item => item.selected = false);
    this.items().filter(item => item.id === id)[0].selected = true;
    this.onSelect.emit(id);
  }
}
