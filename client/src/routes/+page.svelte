<script lang="ts">
	import { onMount } from 'svelte';
	import { goto } from '$app/navigation';
	import Textfield from '@smui/textfield';
	import Button from '@smui/button';
	import Members from '$lib/components/Members.svelte';
	import { Label } from '@smui/common';
	import { NotificationDisplay, notifier } from '@beyonk/svelte-notifications';

	type Member = {
		id: number;
		name: string;
		email: string;
		level: number;
		checked: boolean;
	};

	type EmailChange = {
		oldMember: Omit<Member, 'checked'>;
		newMember: Omit<Member, 'checked'>;
		checked: boolean;
	};

	type UploadResponse = {
		added: Omit<Member, 'checked'>[];
		removed: Omit<Member, 'checked'>[];
		changed: Omit<EmailChange, 'checked'>[];
	};

	let files: FileList | null = null;
	let added: Member[] | null = null;
	let removed: Member[] | null = null;
	let changed: EmailChange[] | null = null;

	onMount(async () => {});

	function goToPDF() {
		goto('/pdf');
	}

	// Note: the change and input events fire before the `files` prop is updated.
	$: if (files != null && files.length) {
		uploadXlsFile(files[0]);
	}

	const uploadXlsFile = async (file: File) => {
		if (files == null) {
			console.log('All values must be provided.');
		} else if (file.type.localeCompare('application/vnd.ms-excel') === 0) {
			const formData = new FormData();
			formData.append('members', file);
			const response = await fetch('/upload', {
				method: 'post',
				body: formData
			});
			if (response.ok) {
				notifier.success('File uploaded successfully');
				files = null;
				const json: UploadResponse = await response.json();
				added = json.added.map((item) => ({ ...item, checked: true }));
				removed = json.removed.map((item) => ({ ...item, checked: true }));
				changed = (json.changed ?? []).map((item) => ({ ...item, checked: true }));
			} else {
				notifier.warning('File failed to upload (not a MyFBO XLS??)');
			}
		} else {
			notifier.danger('File must have an XLS extension!');
		}
	};

	const cancel = async () => {
		added = null;
		removed = null;
		changed = null;
		files = null;
	};

	const submit = async () => {
		if (added == null || removed == null || changed == null) {
			return;
		}

		// added.forEach((item, i) => {
		//   console.log('Added   : ' + item.checked + ' : ' + item.email);
		// });
		// removed.forEach((item, i) => {
		//   console.log('Removed : ' + item.checked + ' : ' + item.email);
		// });

		const json = JSON.stringify({
			added: added,
			removed: removed,
			changed: changed
		});

		const response = await fetch('/update', {
			method: 'post',
			credentials: 'include',
			headers: {
				Accept: 'application/json',
				'Content-Type': 'application/json'
			},
			//make sure to serialize your JSON body
			body: json
		});
		if (!response.ok) {
			console.log('Update of membership failed');
			const json = await response.json();
		} else {
			const json = await response.json();
			cancel();
		}
	};
</script>

<NotificationDisplay />

<div class="center margins">
	<h3>WCFC Mailing List Update</h3>

	{#if !added || !removed || !changed}
		<div class="prompt">Select MyFBO Members File</div>

		<div class="hide-file-ui">
			<!--
        Note: the change and input events fire before the `files` prop is updated.
      -->
			<Textfield bind:files label="File" type="file" input$accept="application/vnd.ms-excel" />
		</div>
	{:else}
		<div class="response">
			{#if added.length > 0 || removed.length > 0 || changed.length > 0}
				<div class="prompt">Membership Changes</div>

				{#if changed.length > 0}
					<div class="warning">
						NOTE: Manual intervention is required to update the email addresses on existing
						groups.io accounts. Changed email addresses will NOT be updated in groups.io. Please
						make a note of these address changes before proceeding and notify the groups.io
						administrators.
					</div>
				{/if}

				<div class="changes">
					{#if added.length > 0}
						<Members label="Added Members" bind:value={added} />
					{/if}

					{#if removed.length > 0}
						<Members label="Removed Members" bind:value={removed} />
					{/if}

					{#if changed.length > 0}
						<div class="members">
							<div class="label">Changed E-mail Addresses</div>
							{#each changed as emailChange}
								<div class="member">
									<input type="checkbox" bind:checked={emailChange.checked} />
									{emailChange.newMember.name}: {emailChange.oldMember.email} -&gt;
									{emailChange.newMember.email}
								</div>
							{/each}
						</div>
					{/if}
				</div>

				<div class="button">
					<Button class="button" variant="outlined" onclick={() => submit()}>
						<Label>Submit Changes</Label>
					</Button>
				</div>
				<div class="button">
					<Button class="button" variant="outlined" onclick={() => cancel()}>
						<Label>Cancel</Label>
					</Button>
				</div>
			{:else}
				<div class="prompt">No Membership Changes Detected</div>
				<div class="button">
					<Button class="button" variant="outlined" onclick={() => cancel()}>
						<Label>Continue</Label>
					</Button>
				</div>
			{/if}
		</div>
	{/if}
</div>

<style>
	.margins {
		margin: 20px;
		margin-left: 100px;
	}
	.changes {
		margin-top: 20px;
		margin-bottom: 25px;
	}
	.members {
		width: fit-content;
	}
	.member {
		margin: 5px;
		font-size: 14px;
	}
	.label {
		font-size: 16px;
		margin-top: 10px;
		border-bottom: solid;
		display: inline-block;
	}
	.warning {
		margin-top: 15px;
		margin-bottom: 15px;
		max-width: 760px;
		border-left: 4px solid #b00020;
		padding: 10px;
		background: #fff7f7;
		color: #5f0011;
		font-size: 14px;
		line-height: 1.4;
	}
	.prompt {
		font-size: 20px;
	}
	.button {
		text-align: left;
		margin-top: 10px;
		margin-right: 10px;
		display: inline-block;
	}
	.response {
		margin-top: 30px;
	}
	.hide-file-ui :global(input[type='file']::file-selector-button) {
		display: none;
	}
	.hide-file-ui :global(:not(.mdc-text-field--label-floating) input[type='file']) {
		color: transparent;
	}
</style>
