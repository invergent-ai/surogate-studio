import { MenuItem } from 'primeng/api';

export const hubMenu = (repoId: string): MenuItem[] => {
  return [
    { label: 'Files', icon: 'pi pi-home', route: `/hub/files/${repoId}`},
    { label: 'Branches', icon: 'pi pi-home', route: `/hub/branches/${repoId}`},
    { label: 'Tags', icon: 'pi pi-home', route: `/hub/tags/${repoId}`},
    { label: 'Commits', icon: 'pi pi-home', route: `/hub/commits/${repoId}`},
    { label: 'Settings', icon: 'pi pi-cog', route: `/hub/settings/${repoId}`}
  ];
};
