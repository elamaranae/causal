<script lang="ts">
  import { type Snippet } from 'svelte';

  let { 
    children, 
    type = 'button', 
    variant = 'primary', 
    class: className = '',
    disabled = false,
    onclick 
  }: { 
    children: Snippet, 
    type?: 'button' | 'submit' | 'reset', 
    variant?: 'primary' | 'secondary' | 'outline' | 'ghost',
    class?: string,
    disabled?: boolean,
    onclick?: (e: MouseEvent) => void
  } = $props();

  const baseStyles = "inline-flex items-center justify-center px-6 py-3 border text-base font-medium rounded-md focus:outline-none focus:ring-2 focus:ring-offset-2 transition-colors duration-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed";
  
  const variants = {
    primary: "border-transparent text-white bg-slate-900 hover:bg-slate-800 focus:ring-slate-900",
    secondary: "border-transparent text-slate-900 bg-slate-100 hover:bg-slate-200 focus:ring-slate-500",
    outline: "border-slate-300 text-slate-700 bg-white hover:bg-slate-50 focus:ring-slate-500",
    ghost: "border-transparent text-slate-700 hover:bg-slate-100 focus:ring-slate-500"
  };
</script>

<button 
  {type}
  class="{baseStyles} {variants[variant]} {className}"
  {disabled}
  {onclick}
>
  {@render children()}
</button>
