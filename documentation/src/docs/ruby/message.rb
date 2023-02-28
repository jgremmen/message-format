# -*- coding: utf-8 -*- #
# frozen_string_literal: true

module Rouge
  module Lexers
    class MessageFormat < RegexLexer
      title 'MESSAGE'
      desc "Message Format"
      tag 'msg'

      nameFragment = /[a-zA-Z][a-zA-Z0-9_]*/
      dashedName = /#{nameFragment}(?:-#{nameFragment})*/
      number = /-?[0-9]+/
      operator = /<>|<=|<|>=|>|!|=/

      state :root do
        rule %r/%{/, Keyword::Variable, :parameter
        mixin :string
      end

      state :whitespace do
        rule %r/\s+/, Text::Whitespace
      end

      state :string do
        rule %r/\\u[0-9a-fA-F]{4,4}/, Str::Escape
        rule %r/\\["'%{\\]/, Str::Escape
        rule %r/[^'"]+/, Text
      end

      state :sq_string do
        rule %r/'/, Str::Single, :pop!
        mixin :string
        rule %r/"/, Text
      end

      state :dq_string do
        rule %r/"/, Str::Double, :pop!
        mixin :string
        rule %r/'/, Text
      end

      state :parameter do
        mixin :whitespace
        rule %r/#{dashedName}/ do
          token Name::Variable
          pop!
          push :parameterFormat
        end
      end

      state :parameterFormat do
        rule %r/(,)(\s*)(#{dashedName})/ do
          groups Punctuation, Text::Whitespace, Name::Namespace
          pop!
          push :parameterConfigKey
        end
        mixin :parameterConfigKey
      end

      state :parameterConfigKey do
        mixin :parameterEnd
        rule %r/(,)(\s*)(#{operator}?)(true|false|null|empty)(\s*)(:)/ do
          groups Punctuation, Text::Whitespace, Operator, Keyword::Constant, Text::Whitespace, Punctuation
          pop!
          push :parameterConfigValue
        end
        rule %r/(,)(\s*)(#{dashedName})(\s*)(:)/ do
          groups Punctuation, Text::Whitespace, Name::Label, Text::Whitespace, Punctuation
          pop!
          push :parameterConfigValue
        end
        rule %r/(,)(\s*)(#{number})(\s*)(:)/ do
          groups Punctuation, Text::Whitespace, Literal::Number, Text::Whitespace, Punctuation
          pop!
          push :parameterConfigValue
        end
        rule %r/(,)(\s*)(')/ do
          groups Punctuation, Text::Whitespace, Str::Single
          pop!
          push :parameterConfigValue
          push :sq_string
        end
        rule %r/(,)(\s*)(")/ do
          groups Punctuation, Text::Whitespace, Str::Double
          pop!
          push :parameterConfigValue
          push :dq_string
        end
        rule %r/(,)(\s*)(:)(\s)*(')/ do
          groups Punctuation, Text::Whitespace, Punctuation, Text::Whitespace, Str::Single
          pop!
          push :parameterConfigKey
          push :sq_message
        end
        rule %r/(,)(\s*)(:)(\s)*(")/ do
          groups Punctuation, Text::Whitespace, Punctuation, Text::Whitespace, Str::Double
          pop!
          push :parameterConfigKey
          push :dq_message
        end
      end

      state :parameterConfigValue do
        mixin :parameterEnd
        rule %r/true|false/ do
          token Keyword::Constant
          pop!
          push :parameterConfigKey
        end
        rule %r/#{number}/ do
          token Number
          pop!
          push :parameterConfigKey
        end
        rule %r/#{dashedName}/ do
          token Str
          pop!
          push :parameterConfigKey
        end
        rule %r/'/ do
          token Str::Single
          pop!
          push :parameterConfigKey
          push :sq_message
        end
        rule %r/"/ do
          token Str::Double
          pop!
          push :parameterConfigKey
          push :dq_message
        end
      end

      state :parameterEnd do
        mixin :whitespace
        rule %r/}/, Keyword, :pop!
      end

      state :sq_message do
        rule %r/%{/, Keyword::Variable, :parameter
        mixin :string
        rule %r/'/, Str::Single, :pop!
      end

      state :dq_message do
        rule %r/%{/, Keyword::Variable, :parameter
        mixin :string
        rule %r/"/, Str::Double, :pop!
      end
    end
  end
end
