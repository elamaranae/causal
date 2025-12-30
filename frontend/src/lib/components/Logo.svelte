<script lang="ts">
	let { class: className = '' }: { class?: string } = $props();
</script>

<a href="/" class="flex items-center gap-2 font-bold text-xl tracking-tight text-slate-900 {className}">
	<div class="w-8 h-8 bg-slate-900 rounded-lg flex items-center justify-center text-white font-serif italic">
		C
	</div>
	<span>causal.</span>
</a>
