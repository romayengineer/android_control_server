<script lang="ts">
	import MouseControl from './lib/MouseControl.svelte';
</script>

<main class="w-full h-screen overflow-hidden">
	<MouseControl />
</main>

<style>
	:global(body) {
		@apply m-0 p-0 overflow-hidden;
	}

	:global(*) {
		@apply box-border;
	}

	main {
		@apply w-full h-screen overflow-hidden;
	}
</style>
