declare module '@beyonk/svelte-notifications' {
	import type { SvelteComponent } from 'svelte';

	export class NotificationDisplay extends SvelteComponent<{}, {}, {}> {}

	export interface Notifier {
		success(message: string): void;
		warning(message: string): void;
		danger(message: string): void;
		info(message: string): void;
	}

	export const notifier: Notifier;
}
