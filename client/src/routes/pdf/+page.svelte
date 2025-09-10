<script lang="ts">
	import { onMount } from 'svelte';
	import { notifier } from '@beyonk/svelte-notifications';

	let data = null;
	let PdfViewer;

	onMount(async () => {
		fetchFile('private/8d2e9806-154b-4461-bd14-078ab3e6fa6b.pdf');
		const module = await import('svelte-pdf');
		PdfViewer = module.default;
	});

	const fetchFile = async (name) => {
		var json = JSON.stringify({
			name: name
		});
		const response = await fetch('https://groundschool.wingsofcarolina.org/api/fetch', {
			method: 'post',
			credentials: 'include',
			headers: {
				Accept: 'application/pdf',
				'Content-Type': 'application/json'
			},
			body: json
		});
		if (!response.ok) {
			notifier.danger('Retrieve of requested document failed.');
		} else {
			data = await response.arrayBuffer();
		}
	};
</script>

<div class="center margins">
	{#if data}
		<svelte:component this={PdfViewer} {data} />
	{/if}
</div>

<style>
	.margins {
		margin: 20px;
		margin-left: 100px;
	}
</style>
