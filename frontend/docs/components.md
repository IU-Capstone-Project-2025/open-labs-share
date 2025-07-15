# Components Documentation

This document describes all reusable UI components in the Open Labs Share frontend.

## Table of Contents

- [ActivityBalance](#activitybalance)
- [ArticleCard](#articlecard)
- [ArticleUpload](#articleupload)
- [BackgroundCircles](#backgroundcircles)
- [ChatWindow](#chatwindow)
- [CommentsSection](#commentssection)
- [GemIcon](#gemicon)
- [LabCard](#labcard)
- [LabUpload](#labupload)
- [Point](#point)
- [Search](#search)
- [Sidebar](#sidebar)
- [Spinner](#spinner)
- [SubmissionCard](#submissioncard)
- [Tags](#tags)
- [ToastNotification](#toastnotification)
- [Tooltip](#tooltip)

## ActivityBalance

**File:** `src/components/ActivityBalance.jsx`

A component that displays user activity statistics in a balanced layout.

### Props

- `stats` (object): User statistics object containing:
  - `labs` (number): Number of labs created
  - `articles` (number): Number of articles created
  - `submissions` (number): Number of submissions made
  - `reviews` (number): Number of reviews completed

### Usage

```jsx
<ActivityBalance stats={{ labs: 5, articles: 3, submissions: 12, reviews: 8 }} />
```

## ArticleCard

**File:** `src/components/ArticleCard.jsx`

A card component for displaying article previews in lists.

### Props

- `article` (object): Article data object
- `onClick` (function): Click handler function

### Usage

```jsx
<ArticleCard article={articleData} onClick={() => navigate(`/articles/${article.id}`)} />
```

## ArticleUpload

**File:** `src/components/ArticleUpload.jsx`

A form component for uploading and creating new articles.

### Props

- `onSubmit` (function): Form submission handler
- `onCancel` (function): Cancel action handler

### Features

- File upload support
- Form validation
- Rich text editing
- Tag management

### Usage

```jsx
<ArticleUpload 
  onSubmit={handleArticleSubmit} 
  onCancel={() => navigate(-1)} 
/>
```

## BackgroundCircles

**File:** `src/components/BackgroundCircles.jsx`

A decorative component that renders animated background circles.

### Usage

```jsx
<BackgroundCircles />
```

## ChatWindow

**File:** `src/components/ChatWindow.jsx`

A chat interface component for AI-powered assistance.

### Props

- `isOpen` (boolean): Controls chat window visibility
- `onClose` (function): Close handler function
- `messages` (array): Array of chat messages
- `onSendMessage` (function): Message sending handler

### Features

- Real-time messaging
- Message history
- AI integration
- Responsive design

### Usage

```jsx
<ChatWindow 
  isOpen={chatOpen}
  onClose={() => setChatOpen(false)}
  messages={chatMessages}
  onSendMessage={handleSendMessage}
/>
```

## CommentsSection

**File:** `src/components/CommentsSection.jsx`

A comprehensive comments system for labs and articles.

### Props

- `contentId` (string): ID of the content being commented on
- `contentType` (string): Type of content ('lab' or 'article')
- `comments` (array): Array of existing comments
- `onCommentAdded` (function): Callback when new comment is added

### Features

- Nested comments
- Rich text editing
- File attachments
- Moderation tools
- Real-time updates

### Usage

```jsx
<CommentsSection 
  contentId="lab-123"
  contentType="lab"
  comments={comments}
  onCommentAdded={handleNewComment}
/>
```

## GemIcon

**File:** `src/components/GemIcon.jsx`

A custom gem-shaped icon component.

### Props

- `className` (string): Additional CSS classes
- `size` (string): Icon size ('sm', 'md', 'lg')

### Usage

```jsx
<GemIcon className="text-blue-500" size="md" />
```

## LabCard

**File:** `src/components/LabCard.jsx`

A card component for displaying lab previews in lists.

### Props

- `lab` (object): Lab data object
- `onClick` (function): Click handler function
- `showActions` (boolean): Whether to show action buttons

### Features

- Lab metadata display
- Status indicators
- Action buttons
- Responsive design

### Usage

```jsx
<LabCard 
  lab={labData} 
  onClick={() => navigate(`/labs/${lab.id}`)}
  showActions={true}
/>
```

## LabUpload

**File:** `src/components/LabUpload.jsx`

A comprehensive form component for uploading and creating new labs.

### Props

- `onSubmit` (function): Form submission handler
- `onCancel` (function): Cancel action handler
- `initialData` (object): Pre-filled form data

### Features

- File upload with drag-and-drop
- Tag management
- Form validation
- Rich text editing
- Preview functionality

### Usage

```jsx
<LabUpload 
  onSubmit={handleLabSubmit}
  onCancel={() => navigate(-1)}
  initialData={existingLabData}
/>
```

## Point

**File:** `src/components/Point.jsx`

A simple point/dot component for visual indicators.

### Props

- `className` (string): Additional CSS classes

### Usage

```jsx
<Point className="bg-green-500" />
```

## Search

**File:** `src/components/Search.jsx`

A search input component with autocomplete functionality.

### Props

- `onSearch` (function): Search handler function
- `placeholder` (string): Search placeholder text
- `className` (string): Additional CSS classes

### Features

- Real-time search
- Autocomplete suggestions
- Keyboard navigation
- Search history

### Usage

```jsx
<Search 
  onSearch={handleSearch}
  placeholder="Search labs and articles..."
  className="w-full"
/>
```

## Sidebar

**File:** `src/components/Sidebar.jsx`

The main navigation sidebar component.

### Props

- `isOpen` (boolean): Controls sidebar visibility
- `onClose` (function): Close handler function
- `user` (object): Current user data

### Features

- Navigation menu
- User profile section
- Theme toggle
- Responsive design
- Authentication status

### Usage

```jsx
<Sidebar 
  isOpen={sidebarOpen}
  onClose={() => setSidebarOpen(false)}
  user={currentUser}
/>
```

## Spinner

**File:** `src/components/Spinner.jsx`

A loading spinner component.

### Props

- `size` (string): Spinner size ('sm', 'md', 'lg')
- `className` (string): Additional CSS classes

### Usage

```jsx
<Spinner size="md" className="text-blue-500" />
```

## SubmissionCard

**File:** `src/components/SubmissionCard.jsx`

A card component for displaying submission previews.

### Props

- `submission` (object): Submission data object
- `onDelete` (function): Delete handler function

### Features

- Submission metadata
- Status indicators
- File attachment display
- Delete confirmation modal

### Usage

```jsx
<SubmissionCard 
  submission={submissionData}
  onDelete={handleDeleteSubmission}
/>
```

## Tags

**File:** `src/components/Tags.jsx`

A component for displaying and managing tags.

### Props

- `tags` (array): Array of tag objects
- `onTagClick` (function): Tag click handler
- `editable` (boolean): Whether tags can be edited
- `onTagAdd` (function): Add tag handler
- `onTagRemove` (function): Remove tag handler

### Features

- Tag display
- Tag editing
- Tag creation
- Tag removal
- Color coding

### Usage

```jsx
<Tags 
  tags={labTags}
  onTagClick={handleTagClick}
  editable={true}
  onTagAdd={handleAddTag}
  onTagRemove={handleRemoveTag}
/>
```

## ToastNotification

**File:** `src/components/ToastNotification.jsx`

A toast notification component for displaying messages.

### Props

- `message` (string): Notification message
- `type` (string): Notification type ('success', 'error', 'warning', 'info')
- `duration` (number): Display duration in milliseconds
- `onClose` (function): Close handler function

### Usage

```jsx
<ToastNotification 
  message="Operation completed successfully!"
  type="success"
  duration={3000}
  onClose={handleClose}
/>
```

## Tooltip

**File:** `src/components/Tooltip.jsx`

A tooltip component for displaying additional information.

### Props

- `content` (string): Tooltip content
- `children` (node): Element to attach tooltip to
- `position` (string): Tooltip position ('top', 'bottom', 'left', 'right')
- `className` (string): Additional CSS classes

### Usage

```jsx
<Tooltip content="This is a helpful tooltip" position="top">
  <button>Hover me</button>
</Tooltip>
```
