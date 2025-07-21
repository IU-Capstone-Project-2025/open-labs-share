# Pages Documentation

This document describes all page components and routing structure in the Open Labs Share frontend.

## Table of Contents

- [Routing Structure](#routing-structure)
- [Public Pages](#public-pages)
- [Protected Pages](#protected-pages)
- [Page Components](#page-components)

## Routing Structure

The application uses React Router v6 with the following route hierarchy:

```
/                           # Landing page (public)
├── /signin                 # Sign in page (public)
├── /signup                 # Sign up page (public)
├── /home                   # Home dashboard (protected)
├── /profile                # User profile (protected)
├── /labs                   # Labs listing (protected)
│   ├── /labs/:id          # Individual lab view (protected)
│   └── /labs/create       # Create lab (protected)
├── /articles               # Articles listing (protected)
│   ├── /articles/:id      # Individual article view (protected)
│   └── /articles/create   # Create article (protected)
├── /submissions            # Submissions listing (protected)
│   └── /submissions/:id   # Individual submission view (protected)
├── /feedback               # Feedback listing (protected)
│   └── /feedback/:id      # Individual feedback view (protected)
├── /review                 # Review queue (protected)
│   └── /review/:id        # Review submission (protected)
└── /search                 # Search results (protected)
```

## Public Pages

### LandingPage

**File:** `src/pages/LandingPage.jsx`
**Route:** `/`

The main landing page for unauthenticated users.

#### Features

- Platform introduction
- Feature highlights
- Call-to-action buttons
- Statistics display
- Navigation to sign in/sign up

#### Components Used

- BackgroundCircles
- Heroicons
- Responsive design elements

### SignIn

**File:** `src/pages/SignIn.jsx`
**Route:** `/signin`

Authentication page for existing users.

#### Features

- Email/username and password login
- Form validation
- Error handling
- Remember me functionality
- Link to sign up page

#### Form Fields

- Email/Username (required)
- Password (required)
- Remember me (optional)

### SignUp

**File:** `src/pages/SignUp.jsx`
**Route:** `/signup`

Registration page for new users.

#### Features

- User registration form
- Real-time validation
- Password strength indicator
- Terms of service acceptance
- Link to sign in page

#### Form Fields

- Username (required)
- Email (required)
- Password (required)
- Confirm Password (required)
- First Name (required)
- Last Name (required)
- Terms acceptance (required)

## Protected Pages

### HomePage

**File:** `src/pages/HomePage.jsx`
**Route:** `/home`

Main dashboard for authenticated users.

#### Features

- User activity overview
- Recent labs and articles
- Quick actions
- Statistics display
- Navigation shortcuts

#### Data Fetched

- User statistics
- Recent labs
- Recent articles
- Platform statistics

### ProfilePage

**File:** `src/pages/ProfilePage.jsx`
**Route:** `/profile`

User profile management page.

#### Features

- Profile information display
- Profile editing
- Password change
- Account statistics
- Content management
- File upload for avatar

#### Sections

- Personal Information
- Account Settings
- My Content (Labs/Articles)
- Statistics
- Danger Zone (Account deletion)

### AllLabsPage

**File:** `src/pages/AllLabsPage.jsx`
**Route:** `/labs`

Public labs listing page.

#### Features

- Labs grid display
- Search and filtering
- Pagination
- Sorting options
- Lab preview cards

#### Filters

- Search by title/description
- Filter by tags
- Sort by date, popularity, rating

### LabPage

**File:** `src/pages/LabPage.jsx`
**Route:** `/labs/:id`

Individual lab view page.

#### Features

- Lab content display
- File downloads
- Comments section
- Submission form
- Lab metadata
- Related labs

#### Components Used

- CommentsSection
- LabUpload (for submissions)
- Tags
- File download components

### CreateLabPage

**File:** `src/pages/CreateLabPage.jsx`
**Route:** `/labs/create`

Lab creation page.

#### Features

- Lab creation form
- File upload
- Tag management
- Preview functionality
- Form validation

#### Redirects to

- LabUpload component for actual form

### MyLabsPage

**File:** `src/pages/MyLabsPage.jsx`
**Route:** `/profile/labs`

User's own labs listing.

#### Features

- Personal labs display
- Lab management actions
- Edit/delete functionality
- Lab statistics
- Quick actions

### AllArticlesPage

**File:** `src/pages/AllArticlesPage.jsx`
**Route:** `/articles`

Public articles listing page.

#### Features

- Articles grid display
- Search and filtering
- Pagination
- Article preview cards
- Tag filtering

### ArticlePage

**File:** `src/pages/ArticlePage.jsx`
**Route:** `/articles/:id`

Individual article view page.

#### Features

- Article content display
- PDF viewer integration
- Comments section
- Article metadata
- Related articles
- Download functionality

#### Components Used

- React PDF viewer
- CommentsSection
- Tags
- ToastNotification

### CreateArticlePage

**File:** `src/pages/CreateArticlePage.jsx`
**Route:** `/articles/create`

Article creation page.

#### Features

- Article creation form
- File upload
- Rich text editing
- Tag management
- Preview functionality

#### Redirects to

- ArticleUpload component for actual form

### MyArticlesPage

**File:** `src/pages/MyArticlesPage.jsx`
**Route:** `/profile/articles`

User's own articles listing.

#### Features

- Personal articles display
- Article management
- Edit/delete functionality
- Article statistics

### MySubmissionsPage

**File:** `src/pages/MySubmissionsPage.jsx`
**Route:** `/profile/submissions`

User's submissions listing.

#### Features

- Submission history
- Status tracking
- Submission details
- Delete functionality
- Submission statistics

### SubmissionPage

**File:** `src/pages/SubmissionPage.jsx`
**Route:** `/submissions/:id`

Individual submission view page.

#### Features

- Submission content display
- File downloads
- Submission metadata
- Status information
- Feedback display
- Comments section

### ReviewQueuePage

**File:** `src/pages/ReviewQueuePage.jsx`
**Route:** `/review`

Review queue for moderators.

#### Features

- Pending submissions list
- Submission previews
- Quick review actions
- Filtering options
- Statistics display

### ReviewSubmissionPage

**File:** `src/pages/ReviewSubmissionPage.jsx`
**Route:** `/review/:id`

Individual submission review page.

#### Features

- Submission content review
- File examination
- Review form
- Feedback submission
- Approval/rejection actions
- Comments and notes

### MyFeedbackPage

**File:** `src/pages/MyFeedbackPage.jsx`
**Route:** `/profile/feedback`

User's feedback listing.

#### Features

- Received feedback display
- Feedback responses
- Feedback statistics
- Filtering options

### FeedbackViewPage

**File:** `src/pages/FeedbackViewPage.jsx`
**Route:** `/feedback/:id`

Individual feedback view page.

#### Features

- Feedback content display
- Response functionality
- Feedback metadata
- Related submissions

### SearchResultsPage

**File:** `src/pages/SearchResultsPage.jsx`
**Route:** `/search`

Search results page.

#### Features

- Search results display
- Result filtering
- Search suggestions
- Result categorization
- Advanced search options

