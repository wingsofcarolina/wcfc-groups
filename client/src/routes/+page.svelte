<script lang="ts">
	import { onMount, tick } from 'svelte';
	import { goto } from '$app/navigation';
	import Textfield from '@smui/textfield';
	import Button from '@smui/button';
	import { Label } from '@smui/common';
	import { NotificationDisplay, notifier } from '@beyonk/svelte-notifications';

	type Member = {
		id: number;
		firstName?: string;
		lastName?: string;
		name: string;
		email: string;
		level: number;
		checked: boolean;
	};

	type DepositsMember = {
		firstName: string;
		lastName: string;
		email: string;
		memberNumber: string;
		fullNameNormalized: string;
		emailNormalized: string;
		numberNormalized: string;
		inactive: boolean;
		checked: boolean;
	};

	type EmailChange = {
		oldMember: Omit<Member, 'checked'>;
		newMember: Omit<Member, 'checked'>;
		checked: boolean;
	};

	type DepositsChange = {
		oldMember: Omit<DepositsMember, 'checked'>;
		newMember: Omit<DepositsMember, 'checked'>;
		checked: boolean;
	};

	type SummaryAction<T> = {
		value: T;
		checked: boolean;
		warning?: boolean;
	};

	type SummaryRow = {
		key: string;
		label: string;
		groups?: SummaryAction<Member | EmailChange>;
		manuals?: SummaryAction<Member | EmailChange>;
		deposits?: SummaryAction<DepositsMember | DepositsChange>;
	};

	type ChangedAppRow = {
		app: string;
		value: string;
	};

	type UploadResponse = {
		added: Omit<Member, 'checked'>[];
		removed: Omit<Member, 'checked'>[];
		changed: Omit<EmailChange, 'checked'>[];
		depositsAdded: Omit<DepositsMember, 'checked'>[];
		depositsRemoved: Omit<DepositsMember, 'checked'>[];
		depositsChanged: DepositsChange[];
	};

	let files: FileList | null = null;
	let added: Member[] | null = null;
	let removed: Member[] | null = null;
	let changed: EmailChange[] | null = null;
	let depositsAdded: DepositsMember[] | null = null;
	let depositsRemoved: DepositsMember[] | null = null;
	let depositsChanged: DepositsChange[] | null = null;
	let addedRows: SummaryRow[] = [];
	let removedRows: SummaryRow[] = [];
	let changedRows: SummaryRow[] = [];
	let isProcessing = false;
	let isSubmitting = false;

	onMount(async () => {});

	function goToPDF() {
		goto('/pdf');
	}

	// Note: the change and input events fire before the `files` prop is updated.
	$: if (files != null && files.length) {
		const file = files[0];
		files = null;
		uploadMembersFile(file);
	}

	const uploadMembersFile = async (file: File) => {
		if (isMembersFile(file)) {
			isProcessing = true;
			await tick();
			const formData = new FormData();
			formData.append('members', file);
			try {
				const response = await fetch('/upload', {
					method: 'post',
					body: formData
				});
				if (response.ok) {
					notifier.success('File uploaded successfully');
					const json: UploadResponse = await response.json();
					added = json.added.map((item) => ({ ...item, checked: true }));
					removed = json.removed.map((item) => ({ ...item, checked: true }));
					changed = (json.changed ?? []).map((item) => ({ ...item, checked: true }));
					depositsAdded = (json.depositsAdded ?? []).map((item) => ({
						...item,
						checked: true
					}));
					depositsRemoved = (json.depositsRemoved ?? []).map((item) => ({
						...item,
						checked: true
					}));
					depositsChanged = (json.depositsChanged ?? []).map((item) => ({
						...item,
						checked: true
					}));
					buildSummaryRows();
				} else {
					notifier.warning('File failed to upload (not a Flight Circle CSV?)');
				}
			} catch (error) {
				console.error('File upload failed', error);
				notifier.danger('File failed to upload');
			} finally {
				isProcessing = false;
			}
		} else {
			notifier.danger('File must have a CSV extension!');
		}
	};

	const isMembersFile = (file: File) => {
		const name = file.name.toLowerCase();
		return (
			name.endsWith('.csv') ||
			name.endsWith('.xls') ||
			file.type === 'text/csv' ||
			file.type === 'application/csv' ||
			file.type === 'application/vnd.ms-excel'
		);
	};

	const cancel = async () => {
		added = null;
		removed = null;
		changed = null;
		depositsAdded = null;
		depositsRemoved = null;
		depositsChanged = null;
		addedRows = [];
		removedRows = [];
		changedRows = [];
		files = null;
	};

	const applied = (action: SummaryAction<unknown> | undefined) =>
		Boolean(action?.checked && !action.warning);

	const remainingRows = (rows: SummaryRow[]) =>
		rows
			.map((row) => ({
				...row,
				groups: applied(row.groups) ? undefined : row.groups,
				manuals: applied(row.manuals) ? undefined : row.manuals,
				deposits: applied(row.deposits) ? undefined : row.deposits
			}))
			.filter((row) => row.groups || row.manuals || row.deposits);

	const showRemainingChanges = () => {
		addedRows = remainingRows(addedRows);
		removedRows = remainingRows(removedRows);
		changedRows = remainingRows(changedRows);
	};

	const submit = async () => {
		if (
			isSubmitting ||
			added == null ||
			removed == null ||
			changed == null ||
			depositsAdded == null ||
			depositsRemoved == null ||
			depositsChanged == null
		) {
			return;
		}

		// added.forEach((item, i) => {
		//   console.log('Added   : ' + item.checked + ' : ' + item.email);
		// });
		// removed.forEach((item, i) => {
		//   console.log('Removed : ' + item.checked + ' : ' + item.email);
		// });

		const json = JSON.stringify(buildUpdateRequest());

		isSubmitting = true;
		try {
			const response = await fetch('/update', {
				method: 'post',
				credentials: 'include',
				headers: {
					Accept: 'application/json',
					'Content-Type': 'application/json'
				},
				body: json
			});
			if (!response.ok) {
				const body = await response.text();
				let message = `Update of membership failed (${response.status})`;
				if (body) {
					try {
						const json = JSON.parse(body);
						message = json.message ?? message;
					} catch {
						console.error(message, body);
					}
				}
				notifier.danger(message);
			} else {
				await response.json();
				showRemainingChanges();
				notifier.success('Membership changes updated');
			}
		} catch (error) {
			console.error('Update of membership failed', error);
			notifier.danger('Update of membership failed');
		} finally {
			isSubmitting = false;
		}
	};

	const depositsName = (member: Pick<DepositsMember, 'firstName' | 'lastName'>) =>
		`${member.firstName} ${member.lastName}`.trim();

	const depositsSummary = (
		member: Pick<DepositsMember, 'firstName' | 'lastName' | 'email' | 'memberNumber' | 'inactive'>
	) =>
		`${depositsName(member)} <${member.email}> #${member.memberNumber}${member.inactive ? ' inactive' : ' active'}`;

	const memberSummary = (member: Pick<Member, 'name' | 'email' | 'id'>) =>
		`${member.name} <${member.email}> #${member.id}`;

	const memberTargetSummary = (member: Pick<Member, 'name' | 'email' | 'id'>) =>
		`${member.name} <${member.email}> #${member.id}`;

	const memberKey = (member: Pick<Member, 'id'>) => normalizeMemberNumber(String(member.id));

	const depositsKey = (member: Pick<DepositsMember, 'memberNumber'>) =>
		normalizeMemberNumber(member.memberNumber);

	const normalizeMemberNumber = (value: string) => {
		const trimmed = value.trim();
		if (/^\d+$/.test(trimmed)) {
			return trimmed.replace(/^0+/, '') || '0';
		}
		return trimmed.toLowerCase();
	};

	const ensureRow = (rows: Map<string, SummaryRow>, key: string, label: string) => {
		let row = rows.get(key);
		if (!row) {
			row = { key, label };
			rows.set(key, row);
		} else if (!row.label) {
			row.label = label;
		}
		return row;
	};

	const buildMemberRows = (
		members: Member[],
		depositsMembers: DepositsMember[],
		actionLabel: 'added' | 'removed'
	) => {
		const rows = new Map<string, SummaryRow>();
		members.forEach((member) => {
			const row = ensureRow(rows, memberKey(member), memberSummary(member));
			row.groups = { value: member, checked: member.checked };
			row.manuals = { value: member, checked: member.checked };
		});
		depositsMembers.forEach((member) => {
			const label =
				actionLabel === 'removed'
					? depositsSummary(member)
					: `${depositsName(member)} <${member.email}> #${member.memberNumber}`;
			const row = ensureRow(rows, depositsKey(member), label);
			row.deposits = { value: member, checked: member.checked };
		});
		return Array.from(rows.values()).sort((a, b) =>
			a.key.localeCompare(b.key, undefined, { numeric: true })
		);
	};

	const buildChangedRows = () => {
		const rows = new Map<string, SummaryRow>();
		(changed ?? []).forEach((emailChange) => {
			const row = ensureRow(
				rows,
				memberKey(emailChange.newMember),
				memberTargetSummary(emailChange.newMember)
			);
			row.groups = { value: emailChange, checked: false, warning: true };
			row.manuals = { value: emailChange, checked: emailChange.checked };
		});
		(depositsChanged ?? []).forEach((depositsChange) => {
			const row = ensureRow(
				rows,
				depositsKey(depositsChange.newMember),
				depositsSummary(depositsChange.newMember)
			);
			row.deposits = { value: depositsChange, checked: depositsChange.checked };
		});
		return Array.from(rows.values()).sort((a, b) =>
			a.key.localeCompare(b.key, undefined, { numeric: true })
		);
	};

	const buildSummaryRows = () => {
		addedRows = buildMemberRows(added ?? [], depositsAdded ?? [], 'added');
		removedRows = buildMemberRows(removed ?? [], depositsRemoved ?? [], 'removed');
		changedRows = buildChangedRows();
	};

	const checkedValues = <T,>(rows: SummaryRow[], system: 'groups' | 'manuals' | 'deposits') =>
		rows
			.map((row) => row[system] as SummaryAction<T> | undefined)
			.filter((action): action is SummaryAction<T> => Boolean(action?.checked && !action.warning))
			.map((action) => ({ ...action.value, checked: true }));

	const buildUpdateRequest = () => ({
		added: [],
		removed: [],
		changed: [],
		groupsAdded: checkedValues<Member>(addedRows, 'groups'),
		groupsRemoved: checkedValues<Member>(removedRows, 'groups'),
		manualsAdded: checkedValues<Member>(addedRows, 'manuals'),
		manualsRemoved: checkedValues<Member>(removedRows, 'manuals'),
		manualsChanged: checkedValues<EmailChange>(changedRows, 'manuals'),
		depositsAdded: checkedValues<DepositsMember>(addedRows, 'deposits'),
		depositsRemoved: checkedValues<DepositsMember>(removedRows, 'deposits'),
		depositsChanged: checkedValues<DepositsChange>(changedRows, 'deposits')
	});

	const hasChanges = () => addedRows.length > 0 || removedRows.length > 0 || changedRows.length > 0;

	const emailChangeFor = (row: SummaryRow) =>
		(row.manuals?.value ?? row.groups?.value) as EmailChange | undefined;

	const depositsChangeFor = (row: SummaryRow) => row.deposits?.value as DepositsChange | undefined;

	const changedAppRows = (row: SummaryRow): ChangedAppRow[] => {
		const emailChange = emailChangeFor(row);
		const depositsChange = depositsChangeFor(row);
		const flightCircleValue = emailChange
			? memberTargetSummary(emailChange.newMember)
			: depositsChange
				? depositsSummary(depositsChange.newMember)
				: row.label;
		const rows: ChangedAppRow[] = [
			{
				app: 'Flight Circle',
				value: flightCircleValue
			}
		];

		if (emailChange) {
			if (row.groups) {
				rows.push({
					app: 'Groups.io',
					value: memberSummary(emailChange.oldMember)
				});
			}
			if (row.manuals) {
				rows.push({
					app: 'Manuals',
					value: memberSummary(emailChange.oldMember)
				});
			}
		}

		if (depositsChange) {
			rows.push({
				app: 'Deposits',
				value: depositsSummary(depositsChange.oldMember)
			});
		}

		return rows;
	};
</script>

<svelte:head>
	<title>WCFC Membership Sync</title>
</svelte:head>

<NotificationDisplay />

<header class="topbar">
	<h1>WCFC Membership Sync</h1>
</header>

<main class="content">
	{#if isProcessing}
		<div class="prompt processing" role="status">Processing...</div>
	{:else if !added || !removed || !changed || !depositsAdded || !depositsRemoved || !depositsChanged}
		<div class="prompt">Select Flight Circle Members File</div>

		<div class="hide-file-ui">
			<!--
        Note: the change and input events fire before the `files` prop is updated.
      -->
			<Textfield
				bind:files
				label="File"
				type="file"
				input$accept=".csv,text/csv,application/vnd.ms-excel"
			/>
		</div>
	{:else}
		<div class="response">
			{#if hasChanges()}
				<div class="prompt">Membership Changes</div>

				<div class="changes">
					{#if addedRows.length > 0}
						<div class="change-section">
							<div class="label">Added</div>
							<table class="change-table">
								<thead>
									<tr>
										<th>Member</th>
										<th>Groups.io</th>
										<th>Manuals</th>
										<th>Deposits</th>
									</tr>
								</thead>
								<tbody>
									{#each addedRows as row}
										<tr>
											<td>{row.label}</td>
											<td
												>{#if row.groups}<input
														type="checkbox"
														bind:checked={row.groups.checked}
													/>{/if}</td
											>
											<td
												>{#if row.manuals}<input
														type="checkbox"
														bind:checked={row.manuals.checked}
													/>{/if}</td
											>
											<td
												>{#if row.deposits}<input
														type="checkbox"
														bind:checked={row.deposits.checked}
													/>{/if}</td
											>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}

					{#if removedRows.length > 0}
						<div class="change-section">
							<div class="label">Removed</div>
							<table class="change-table">
								<thead>
									<tr>
										<th>Member</th>
										<th>Groups.io</th>
										<th>Manuals</th>
										<th>Deposits</th>
									</tr>
								</thead>
								<tbody>
									{#each removedRows as row}
										<tr>
											<td>{row.label}</td>
											<td
												>{#if row.groups}<input
														type="checkbox"
														bind:checked={row.groups.checked}
													/>{/if}</td
											>
											<td
												>{#if row.manuals}<input
														type="checkbox"
														bind:checked={row.manuals.checked}
													/>{/if}</td
											>
											<td
												>{#if row.deposits}<input
														type="checkbox"
														bind:checked={row.deposits.checked}
													/>{/if}</td
											>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}

					{#if changedRows.length > 0}
						<div class="change-section">
							<div class="label">Changed</div>
							<table class="change-table">
								<thead>
									<tr>
										<th>Member</th>
										<th>Groups.io</th>
										<th>Manuals</th>
										<th>Deposits</th>
									</tr>
								</thead>
								<tbody>
									{#each changedRows as row}
										<tr>
											<td>
												<div class="changed-member-grid">
													{#each changedAppRows(row) as appRow}
														<div class="changed-app">{appRow.app}</div>
														<div>{appRow.value}</div>
													{/each}
												</div>
											</td>
											<td>
												{#if row.groups?.warning}
													<span
														class="manual-warning"
														title="This email address must be updated manually in groups.io">!</span
													>
												{:else if row.groups}
													<input type="checkbox" bind:checked={row.groups.checked} />
												{/if}
											</td>
											<td
												>{#if row.manuals}<input
														type="checkbox"
														bind:checked={row.manuals.checked}
													/>{/if}</td
											>
											<td
												>{#if row.deposits}<input
														type="checkbox"
														bind:checked={row.deposits.checked}
													/>{/if}</td
											>
										</tr>
									{/each}
								</tbody>
							</table>
						</div>
					{/if}
				</div>

				<div class="button">
					<Button
						class="button"
						variant="outlined"
						disabled={isSubmitting}
						onclick={() => submit()}
					>
						<Label>{isSubmitting ? 'Updating...' : 'Submit Changes'}</Label>
					</Button>
				</div>
				<div class="button">
					<Button
						class="button"
						variant="outlined"
						disabled={isSubmitting}
						onclick={() => cancel()}
					>
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
</main>

<style>
	:global(body) {
		background: #f3f6f8;
		color: #142438;
		margin: 0;
	}
	.topbar {
		align-items: center;
		background: #d8e9f1;
		border-bottom: 3px solid #6f9ab4;
		box-shadow: 0 2px 8px rgba(20, 36, 56, 0.12);
		display: flex;
		min-height: 68px;
		padding: 0 48px;
	}
	.topbar h1 {
		font-size: 24px;
		font-weight: 500;
		margin: 0;
	}
	.content {
		margin: 30px auto;
		max-width: 1180px;
		padding: 0 28px;
	}
	.changes {
		margin-top: 20px;
		margin-bottom: 25px;
	}
	.change-section {
		margin-top: 18px;
		max-width: 980px;
	}
	.change-table {
		border-collapse: collapse;
		margin-top: 8px;
		width: 100%;
		font-size: 14px;
	}
	.change-table th,
	.change-table td {
		border-bottom: 1px solid #ddd;
		padding: 7px 10px;
		text-align: left;
		vertical-align: top;
	}
	.change-table th:not(:first-child),
	.change-table td:not(:first-child) {
		text-align: center;
		width: 92px;
	}
	.changed-member-grid {
		display: grid;
		grid-template-columns: 96px minmax(260px, 1fr);
		gap: 4px 10px;
		line-height: 1.25;
	}
	.changed-app {
		color: #4b5563;
		font-weight: 700;
	}
	.label {
		font-size: 16px;
		margin-top: 10px;
		border-bottom: solid;
		display: inline-block;
	}
	.manual-warning {
		background: #f7c948;
		border-radius: 50%;
		color: #3b2f00;
		display: inline-block;
		font-weight: 700;
		height: 20px;
		line-height: 20px;
		text-align: center;
		width: 20px;
	}
	.prompt {
		font-size: 20px;
	}
	.processing {
		color: #455867;
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
	@media (max-width: 640px) {
		.topbar {
			padding: 0 20px;
		}
		.topbar h1 {
			font-size: 21px;
		}
		.content {
			margin-top: 24px;
			padding: 0 16px;
		}
	}
</style>
