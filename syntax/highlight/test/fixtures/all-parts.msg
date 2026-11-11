%{count,format:choice,0:'no items',1:'one item',:'%{count} items'} in %[folder,name->folder-name,
  shared=true] %(clip,'%{description,format:string}',size:40,ellipsis:'…')
