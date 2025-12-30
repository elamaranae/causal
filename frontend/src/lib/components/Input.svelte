<script lang="ts">
	let { 
		id, 
		label, 
		type = 'text', 
		placeholder = '', 
		value = $bindable(), 
		required = false,
		class: className = '' 
	}: { 
		id: string, 
		label: string, 
		type?: string, 
		placeholder?: string, 
		value: string, 
		required?: boolean, 
		class?: string 
	} = $props();
</script>

<div class="w-full {className}">
	<label for={id} class="block text-sm font-medium text-slate-700 mb-1">{label}</label>
	<input
		{id}
		{type}
		{placeholder}
		bind:value
		{required}
		class="appearance-none block w-full px-3 py-2 border border-slate-300 rounded-md shadow-sm placeholder-slate-400 focus:outline-none focus:ring-slate-500 focus:border-slate-500 sm:text-sm transition-colors duration-200"
	/>
</div>
