<script lang="ts">
  import { onMount } from 'svelte';
  import Textfield from '@smui/textfield';
  import Button from '@smui/button';
  import IconButton from '@smui/icon-button';
  import Members from "$lib/components/Members.svelte"
  import { Icon, Label } from '@smui/common';
  import { NotificationDisplay, notifier } from '@beyonk/svelte-notifications'

  let valueTypeNumber = 0;
  let valueTypeNumberStep = 0;
  let valueTypeDate = '';
  let files: FileList | null = null;

  $: added = null;
  $: removed = null;

  onMount(function() {
  });

  // Note: the change and input events fire before the `files` prop is updated.
  $: if (files != null && files.length) {
    uploadXlsFile(files[0])
  }

  const uploadXlsFile = async (file) => {
    if (files == null) {
      console.log("All values must be provided.")
    } else if (file.type.localeCompare('application/vnd.ms-excel') == 0) {
      const formData = new FormData();
      formData.append('members', file);
      const response = await fetch('/upload', {
          method: 'post',
          body: formData
      });
      if (response.ok) {
        notifier.success('File uploaded successfully');
        files = null;
        const json = await response.json();
        added = [];
        removed = [];
        json['added'].forEach((item, i) => {
          item.checked = true;
          added.push(item);
        });
        json['removed'].forEach((item, i) => {
          item.checked = true;
          removed.push(item);
        });
      } else {
        notifier.warning('File failed to upload (not a MyFBO XLS??)');
      }
    } else {
      notifier.danger('File must have an XLS extension!');
    }
  }

  const cancel = async () => {
    added = null;
    removed = null;
    files = null;
  }

  const submit = async () => {
    // added.forEach((item, i) => {
    //   console.log('Added   : ' + item.checked + ' : ' + item.email);
    // });
    // removed.forEach((item, i) => {
    //   console.log('Removed : ' + item.checked + ' : ' + item.email);
    // });

    var json = JSON.stringify({
      added: added,
      removed: removed
    });

    const response = await fetch( '/update', {
      method: "post",
      withCredentials: true,
      headers: {
        'Accept': 'application/json',
        'Content-Type': 'application/json'
      },
      //make sure to serialize your JSON body
      body: json
    });
    if ( ! response.ok) {
      console.log('Update of membership failed');
      const json = await response.json();
    } else {
      const json = await response.json();
      cancel();
    }
  }
</script>

<NotificationDisplay />

<div class="center margins">
  <h3>WCFC Mailing List Update</h3>

  {#if !added || !removed}
    <div class=prompt>Select MyFBO Members File</div>

    <div class="hide-file-ui">
      <!--
        Note: the change and input events fire before the `files` prop is updated.
      -->
      <Textfield bind:files={files} label="File" type="file"
          inputProps={{accept:"application/vnd.ms-excel"}} />
    </div>
  {:else}
    <div class=response>
      {#if added.length > 0 || removed.length > 0}
        <div class=prompt>Membership Changes</div>

        <div class=changes>
          {#if added.length > 0}
            <Members label='Added Members' bind:value={added} />
          {/if}

          {#if removed.length > 0}
            <Members label='Removed Members' bind:value={removed} />
          {/if}
        </div>

        <div class=button >
          <Button class=button variant="outlined" on:click={() => submit()}>
            <Label>Submit Changes</Label>
          </Button>
        </div>
        <div class=button >
          <Button class=button variant="outlined" on:click={() => cancel()}>
            <Label>Cancel</Label>
          </Button>
        </div>
      {:else}
        <div class=prompt>No Membership Changes Detected</div>
        <div class=button >
          <Button class=button variant="outlined" on:click={() => cancel()}>
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
  .hide-file-ui
    :global(:not(.mdc-text-field--label-floating) input[type='file']) {
    color: transparent;
  }
</style>
