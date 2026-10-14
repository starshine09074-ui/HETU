<script setup>
import {onMounted,ref,watch} from 'vue'
import {Bold,Italic,Underline,Heading2,List,ListOrdered,Quote,Link,Minus} from '@lucide/vue'

const props=defineProps({modelValue:{type:String,default:''},placeholder:{type:String,default:'在这里编写需求背景、目标和验收标准…'}})
const emit=defineEmits(['update:modelValue','paste-image'])
const editor=ref(null)
let savedRange=null
onMounted(()=>{editor.value.innerHTML=props.modelValue||''})
watch(()=>props.modelValue,value=>{if(editor.value&&editor.value.innerHTML!==value)editor.value.innerHTML=value||''})
function sync(){emit('update:modelValue',editor.value?.innerHTML||'')}
function command(name,value=null){editor.value?.focus();document.execCommand(name,false,value);sync()}
function setLink(){const value=window.prompt('输入链接地址','https://');if(value)command('createLink',value)}
function handlePaste(event){const item=Array.from(event.clipboardData?.items||[]).find(x=>x.type.startsWith('image/'));if(!item)return;const file=item.getAsFile();if(!file)return;event.preventDefault();const selection=window.getSelection();if(selection?.rangeCount)savedRange=selection.getRangeAt(0).cloneRange();emit('paste-image',file)}
function insertHtml(html){editor.value?.focus();const selection=window.getSelection();let range=savedRange;if(!range&&selection?.rangeCount)range=selection.getRangeAt(0);if(!range||!editor.value?.contains(range.commonAncestorContainer)){range=document.createRange();range.selectNodeContents(editor.value);range.collapse(false)}range.deleteContents();const holder=document.createElement('template');holder.innerHTML=html;const fragment=holder.content;const last=fragment.lastChild;range.insertNode(fragment);if(last){range.setStartAfter(last);range.collapse(true);selection?.removeAllRanges();selection?.addRange(range)}savedRange=null;sync()}
defineExpose({insertHtml})
</script>

<template>
  <div class="rich-editor">
    <div class="rich-editor-toolbar" role="toolbar" aria-label="富文本工具栏">
      <button type="button" title="粗体" aria-label="粗体" @mousedown.prevent="command('bold')"><Bold :size="15"/></button>
      <button type="button" title="斜体" aria-label="斜体" @mousedown.prevent="command('italic')"><Italic :size="15"/></button>
      <button type="button" title="下划线" aria-label="下划线" @mousedown.prevent="command('underline')"><Underline :size="15"/></button>
      <i></i>
      <button type="button" title="标题" aria-label="标题" @mousedown.prevent="command('formatBlock','h2')"><Heading2 :size="15"/></button>
      <button type="button" title="项目符号列表" aria-label="项目符号列表" @mousedown.prevent="command('insertUnorderedList')"><List :size="15"/></button>
      <button type="button" title="编号列表" aria-label="编号列表" @mousedown.prevent="command('insertOrderedList')"><ListOrdered :size="15"/></button>
      <button type="button" title="引用" aria-label="引用" @mousedown.prevent="command('formatBlock','blockquote')"><Quote :size="15"/></button>
      <button type="button" title="插入链接" aria-label="插入链接" @mousedown.prevent="setLink"><Link :size="15"/></button>
      <button type="button" title="分隔线" aria-label="分隔线" @mousedown.prevent="command('insertHorizontalRule')"><Minus :size="15"/></button>
    </div>
    <div ref="editor" class="rich-editor-content" contenteditable="true" role="textbox" aria-multiline="true" :data-placeholder="placeholder" @paste="handlePaste" @input="sync" @blur="sync"></div>
  </div>
</template>
