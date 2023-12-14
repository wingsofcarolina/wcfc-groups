<script>
  import { Icon, Label } from '@smui/common';
  import Button from '@smui/button';

  export let label;
  export let value;
</script>

<div class=members>
  <div class=label>{label}</div>
  {#each value as member}
    <div class=member>
      <input type='checkbox' bind:checked={member.checked}>
      {member.name} &lt;{member.email}&gt;
    </div>
  {/each}
</div>

<style>
.members {
  width: fit-content;
}
.member {
  margin: 5px;
}
.label {
  font-size: 15px;
  margin-top: 10px;
  border-bottom: solid;
  display: inline-block;
}
</style>
