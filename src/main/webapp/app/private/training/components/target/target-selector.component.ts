import { Component, computed, OnInit, Signal } from '@angular/core';
import { CardModule } from 'primeng/card';
import { NgForOf, NgTemplateOutlet } from '@angular/common';
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
    NgForOf
  ]
})
export class TargetSelectorComponent implements OnInit {
  items: Signal<any[]> = computed(() => [
    {
      imgBg: "bg-purple-400",
      img: ServerIcon,
      title: "Local infrastructure",
      description: "Training jobs will be distributed across your multi-node, multi-GPU infrastructure using the current k8s cluster.",
      tags: [
        {severity: "success", value: "On-Prem", image: Check},
        {severity: "secondary", value: "Private", image: Check},
        {severity: "warning", value: "Powered by KubeRay", image: Rocket},
      ],
    },
    {
      imgBg: "bg-orange-500",
      img: Rocket,
      title: "In the cloud",
      description: "Training jobs will be launched on configured public cloud compute resources.",
      tags: [
        {severity: "success", value: "Fast", image: Check},
        {severity: "secondary", value: "Unlimited resources", image: Check},
        {severity: "warning", value: "Powered by SkyPilot", image: Rocket},
      ],
    },
  ]);

  ngOnInit() {}
}
